package ru.skelantros.easymacher.doobieimpl.word

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.{DbResult, DbUnit, UserDb}
import ru.skelantros.easymacher.db.WordDb._
import ru.skelantros.easymacher.doobieimpl.word.WordQueries._
import ru.skelantros.easymacher.doobieimpl.word.{Gender => DbGender}
import ru.skelantros.easymacher.entities.{AnyWord, Noun, Word}
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._

class WordDoobie[F[_] : Async](implicit xa: Transactor[F], userDb: UserDb.Select[F])
  extends Select[F] with AddAny[F] with AddNoun[F] {

  private def baseNoteToWord(note: BaseNote): F[DbResult[Word]] =
    userDb.userById(note.userId).map{ userRes =>
      userRes.map(u => AnyWord(note.id, note.word, note.translate, u))
    }

  private def nounNoteToWord(note: NounNote): F[DbResult[Word]] =
    userDb.userById(note.userId).map { userRes =>
      userRes.map(u => Noun(note.id, note.word, note.translate, u, note.gender.entityGender, note.plural))
    }

  override def allWords: F[DbResult[Seq[Word]]] = {
    val query = for {
      anyWords <- selectAllBase.to[Seq]
      nounWords <- selectAllNouns.to[Seq]
    } yield (anyWords.map(baseNoteToWord) ++ nounWords.map(nounNoteToWord)).sequence

    query.attempt.transact(xa).flatMap {
      case Right(x) => x.map(_.sequence)
      case Left(t) => DbResult.thr[Seq[Word]](t).pure[F]
    }
  }

  override def wordById(id: Int): F[DbResult[Word]] = {
    val query = for {
      anyWord <- selectByIdBase(id).option
      res <- anyWord match {
        case Some(x) => baseNoteToWord(x).pure[ConnectionIO]
        case None => selectByIdNouns(id).map(nounNoteToWord).unique
      }
    } yield res

    query.attempt.transact(xa).flatMap {
      case Right(x) => x
      case Left(t) => DbResult.thr[Word](t).pure[F]
    }
  }

  override def wordsByUserId(userId: Int): F[DbResult[Seq[Word]]] = {
    val query = for {
      anyWords <- selectAllByUserIdBase(userId).to[Seq]
      nounWords <- selectAllByUserIdNouns(userId).to[Seq]
    } yield (anyWords.map(baseNoteToWord) ++ nounWords.map(nounNoteToWord)).sequence

    query.attempt.transact(xa).flatMap {
      case Right(x) => x.map(_.sequence)
      case Left(t) => DbResult.thr[Seq[Word]](t).pure[F]
    }
  }

  override def addWord(word: String, translate: Option[String], userId: Int): F[DbUnit] = {
    val query = insertBase(userId, word, translate, None)

    query.run.attempt.transact(xa).map {
      case Right(_) => DbResult.unit
      case Left(t) => DbResult.thr(t)
    }
  }

  override def addNoun(word: String, translate: Option[String], gender: Noun.Gender, plural: Option[String], userId: Int): F[DbUnit] = {
    val query = for {
      base <- insertBase(userId, word, translate, None).baseNote
      wordId = base.id
      noun <- insertNoun(wordId, plural, DbGender(gender)).run
    } yield ()

    query.attempt.transact(xa).map {
      case Right(()) => DbResult.unit
      case Left(t) => DbResult.thr(t)
    }
  }
}
