package ru.skelantros.easymacher.db

import cats.Monad
import cats.implicits._
import ru.skelantros.easymacher.entities.{AnyWord, Noun, Word}
import ru.skelantros.easymacher.db.WordDb._

import scala.collection.mutable.ArrayBuffer

class WordMock[F[_] : Monad](init: Seq[Word], val userDb: UserDb.Select[F])
  extends Select[F] with AddAny[F] with AddNoun[F] {
  private val db = ArrayBuffer.from(init)

  override def allWords: F[DbResult[Seq[Word]]] = DbResult.of(db.toSeq).pure[F]

  private def nextId: Int = db.maxBy(_.id).id + 1

  override def wordById(id: Int): F[DbResult[Word]] = Monad[F].pure {
    db.find(_.id == id) match {
      case Some(w) => DbResult.of(w)
      case None => DbResult.mistake(s"Word with id $id does not exist.")
    }
  }

  override def wordsByUserId(userId: Int): F[DbResult[Seq[Word]]] =
    DbResult.of(db.filter(_.owner.id == userId).toSeq).pure[F]

  override def addWord(word: String, translate: Option[String], userId: Int): F[DbUnit] =
    for {
      u <- userDb.userById(userId)
      res: DbUnit = u match {
        case Right(user) =>
          db += AnyWord(nextId, word, translate, user)
          DbResult.unit
        case Left(error) => Left[Error, Unit](error)
      }
    } yield res

  override def addNoun(word: String, translate: Option[String], gender: Noun.Gender, plural: Option[String], userId: Int): F[DbUnit] =
    for {
      u <- userDb.userById(userId)
      res: DbUnit = u match {
        case Right(user) => db += Noun(nextId, word, translate, user, gender, plural); DbResult.unit
        case Left(error) => Left[Error, Unit](error)
      }
    } yield res
}
