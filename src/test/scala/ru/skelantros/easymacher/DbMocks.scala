package ru.skelantros.easymacher

import cats.Monad
import cats.implicits._
import scala.collection.mutable
import ru.skelantros.easymacher.db.{DbResult, UserDb}
import ru.skelantros.easymacher.entities.{Role, User}

object DbMocks {
  def userDbSelect[F[_] : Monad](init: Seq[User]) =
    new UserDb.Select[F] with UserDb.SelectOffset[F] {
      private val db = mutable.ArrayBuffer.from(init)

      override def allUsers: F[DbResult[Seq[User]]] =
        DbResult.of(db.toSeq).pure[F]

      override def usersByRole(role: Role): F[DbResult[Seq[User]]] =
        DbResult.of(db.filter(_.role == role).toSeq).pure[F]

      override def userById(id: Int): F[DbResult[User]] = Monad[F].pure {
        db.find(_.id == id) match {
          case Some(x) => DbResult.of(x)
          case None => DbResult.mistake(s"User with id $id does not exist.")
        }
      }

      override def userByUsername(username: String): F[DbResult[User]] = Monad[F].pure {
        db.find(_.username == username) match {
          case Some(x) => DbResult.of(x)
          case None => DbResult.mistake(s"User with username '$username' does not exist.")
        }
      }

      override def allUsers(from: Int, count: Int): F[DbResult[Seq[User]]] =
        DbResult.of(db.slice(from, from + count).toSeq).pure[F]

      override def usersByRole(role: Role)(from: Int, count: Int): F[DbResult[Seq[User]]] =
        DbResult.of(db.slice(from, from + count).toSeq).pure[F]
    }
}
