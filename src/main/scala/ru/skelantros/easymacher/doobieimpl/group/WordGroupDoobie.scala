package ru.skelantros.easymacher.doobieimpl.group

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.{DbResult, DbUnit, UserDb, WordDb}
import doobie.implicits.toConnectionIOOps
import cats.effect.implicits._
import cats.implicits._
import ru.skelantros.easymacher.db.WordGroupDb._
import ru.skelantros.easymacher.doobieimpl._
import GroupQueries._
import cats.data.EitherT
import ru.skelantros.easymacher.entities.{Word, WordGroup}
import ru.skelantros.easymacher.utils.StatusMessages

class WordGroupDoobie[F[_] : Async](implicit val xa: Transactor[F], wordDb: WordDb.Select[F], userDb: UserDb.Select[F])
  extends DescSelect[F] with Select[F] with Update[F] {

  override def allDescs: F[DbResult[Seq[WordGroup.Desc]]] =
    selectGroups.to[Seq].attempt.transact(xa).map {
      case Right(seq) => DbResult.of(seq.map(_.toDesc))
      case Left(t) => DbResult.thr(t)
    }

  override def descById(id: Int): F[DbResult[WordGroup.Desc]] =
    processOptSelect(selectGroupById(id).option)(_.toDesc, StatusMessages.noGroupById(id))

  private def wordGroup(gNote: GroupNote, wordsIds: Seq[Int]): F[DbResult[WordGroup]] = {
    val userRes = EitherT(userDb.userById(gNote.ownerId))
    val wordsRes = EitherT(wordsIds.map(wordDb.wordById).sequence.map(_.sequence))
    val groupRes = for {
      user <- userRes
      words <- wordsRes
    } yield WordGroup(gNote.id, user, gNote.name, gNote.isShared, words)
    groupRes.value
  }

  override def allGroups: F[DbResult[Seq[WordGroup]]] = {
    val query = for {
      groupNotes <- selectGroups.to[Seq]
      wordsIds <- groupNotes.map(n => selectG2WByGroupId(n.id).map(_.wordId).to[Seq]).sequence
    } yield groupNotes zip wordsIds

    query.attempt.transact(xa).flatMap {
      case Right(seq) => seq.map(t => wordGroup(t._1, t._2)).sequence.map(_.sequence)
      case Left(t) => DbResult.thr[Seq[WordGroup]](t).pure[F]
    }
  }

  override def groupWithWordsById(id: Int): F[DbResult[WordGroup]] = {
    val query = for {
      groupNote <- selectGroupById(id).option
      wordsIds <- selectG2WByGroupId(id).map(_.wordId).to[Seq]
    } yield (groupNote, wordsIds)

    query.attempt.transact(xa).flatMap {
      case Right((Some(note), wordsIds)) => wordGroup(note, wordsIds)
      case Right((None, _)) => DbResult.mistake[WordGroup](StatusMessages.noGroupById(id)).pure[F]
      case Left(t) => DbResult.thr[WordGroup](t).pure[F]
    }
  }

  override def createGroup(userId: Int, name: String, isShared: Boolean): F[DbUnit] =
    processInsert(insertGroup(userId, name, isShared))

  // TODO слова в данном запросе отправляются в рамках одной транзакции, возможно стоит переделать
  override def addWordsByIds(id: Int, wordsIds: Seq[Int]): F[DbUnit] = {
    val query = for {
      _ <- wordsIds.map(insertG2W(id, _).run.void).sequence
    } yield ()

    processSelect(query)(identity)
  }

  override def update(id: Int, name: Option[String], isShared: Option[Boolean]): F[DbUnit] =
    processInsert(updateGroup(id, name, isShared))
}
