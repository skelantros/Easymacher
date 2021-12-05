package ru.skelantros.easymacher.doobieimpl.flashcard

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.FlashCardDb._
import ru.skelantros.easymacher.db.{DbError, DbResult, DbUnit, UserDb, WordDb, WordGroupDb}
import ru.skelantros.easymacher.doobieimpl._
import ru.skelantros.easymacher.entities.FlashCards
import doobie.implicits.toConnectionIOOps
import doobie.implicits.toDoobieApplicativeErrorOps
import cats.implicits._
import FlashCardQueries._
import FlashCards.{FlashDesc => Desc}
import cats.data.EitherT
import ru.skelantros.easymacher.utils.StatusMessages

class FlashCardDoobie[F[_] : Async](implicit val xa: Transactor[F],
                                    wordDb: WordDb.Select[F], userDb: UserDb.Select[F], groupDb: WordGroupDb.Select[F])
  extends DescSelect[F] with Select[F] with Update[F] {
  override def allDescs: F[DbResult[Seq[Desc]]] =
    selectCards.to[Seq].attempt.transact(xa).map {
      case Right(seq) => DbResult.of(seq.map(_.toDesc))
      case Left(t) => DbResult.thr(t)
    }

  override def descById(id: Int): F[DbResult[Desc]] =
    processOptSelect(selectCardsById(id).option)(_.toDesc, StatusMessages.noCardsById(id))

  private def flashCards(note: FCNote, wordsIds: Seq[Int]): F[DbResult[FlashCards]] = {
    val userRes = EitherT(userDb.userById(note.ownerId))
    val wordsRes = EitherT(wordsIds.map(wordDb.wordById).sequence.map(_.sequence))
    val groupRes = for {
      user <- userRes
      words <- wordsRes
    } yield FlashCards(note.id, note.name, user, note.isShared, words)
    groupRes.value
  }

  override def allFlashCards: F[DbResult[Seq[FlashCards]]] = {
    val query = for {
      notes <- selectCards.to[Seq]
      wordsIds <- notes.map(n => selectFC2WByCardsId(n.id).map(_.wordId).to[Seq]).sequence
    } yield notes zip wordsIds

    query.attempt.transact(xa).flatMap {
      case Right(seq) => seq.map(t => flashCards(t._1, t._2)).sequence.map(_.sequence)
      case Left(t) => DbResult.thr[Seq[FlashCards]](t).pure[F]
    }
  }

  override def flashCardsById(id: Int): F[DbResult[FlashCards]] = {
    val query = for {
      groupNote <- selectCardsById(id).option
      wordsIds <- selectFC2WByCardsId(id).map(_.wordId).to[Seq]
    } yield (groupNote, wordsIds)

    query.attempt.transact(xa).flatMap {
      case Right((Some(note), wordsIds)) => flashCards(note, wordsIds)
      case Right((None, _)) => DbResult.mistake[FlashCards](StatusMessages.noGroupById(id)).pure[F]
      case Left(t) => DbResult.thr[FlashCards](t).pure[F]
    }
  }

  override def createFlashCards(userId: Int, name: String, isShared: Boolean): F[DbUnit] =
    processInsert(insertCards(userId, name, isShared))

  override def addWordsByIds(id: Int, wordsSeq: Seq[Int]): F[DbUnit] = {
    val singleQuery = (wordId: Int) => insertFC2W(id, wordId).run.void.attemptSomeSqlState {
      case PsqlStates.uniqueViolation => StatusMessages.wordAleadyInGroup(wordId, id)
      case PsqlStates.foreignKeyViolation => StatusMessages.noWordById(wordId)
    }

    val query = for {
      res <- wordsSeq.map(singleQuery).sequence
    } yield res.sequence

    processSelectUnitEither(query)
  }

  override def addWordsByGroupId(id: Int, groupId: Int): F[DbUnit] = {
    val query = for {
      groupRes <- groupDb.groupWithWordsById(groupId)
    } yield groupRes.map(_.words.map(_.id))

    query.flatMap {
      case Right(seq) => addWordsByIds(id, seq)
      case Left(error) => Either.left[DbError, Unit](error).pure[F]
    }
  }

  override def update(id: Int, name: Option[String], isShared: Option[Boolean]): F[DbUnit] =
    processInsert(updateCards(id, name, isShared))
}
