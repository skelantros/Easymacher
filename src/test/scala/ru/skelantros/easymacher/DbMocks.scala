package ru.skelantros.easymacher

import cats.Monad
import cats.implicits._

import scala.collection.mutable
import ru.skelantros.easymacher.db.{DbResult, DbUnit, UserDb}
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email

object DbMocks {
  def userDbSelect[F[_] : Monad](init: Seq[User]) =
    new UserDb.Select[F] with UserDb.SelectOffset[F] with UserDb.Update[F] {
      private val db = mutable.ArrayBuffer.from[User](init)

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
        db.find(_.username.toLowerCase == username.toLowerCase) match {
          case Some(x) => DbResult.of(x)
          case None => DbResult.mistake(s"User with username '$username' does not exist.")
        }
      }

      override def userByEmail(email: Email): F[DbResult[User]] = Monad[F].pure {
        db.find(_.email.toLowerCase == email.asString.toLowerCase) match {
          case Some(x) => DbResult.of(x)
          case None => DbResult.mistake(s"User with email '$email' does not exist.")
        }
      }

      override def allUsers(from: Int, count: Int): F[DbResult[Seq[User]]] =
        DbResult.of(db.slice(from, from + count).toSeq).pure[F]

      override def usersByRole(role: Role)(from: Int, count: Int): F[DbResult[Seq[User]]] =
        DbResult.of(db.slice(from, from + count).toSeq).pure[F]

      override def updatePassword(id: Int, oldPassword: String, newPassword: String): F[DbUnit] = Monad[F].pure {
        val userOpt = db.zipWithIndex.find(_._1.id == id)
        userOpt match {
          case Some((user, idx)) =>
            val password = user.password
            if(password == oldPassword) {
              db(idx) = user.copy(password = newPassword)
              DbResult.unit
            } else DbResult.mistake("Wrong password.")
          case None => DbResult.mistake(s"User with id $id does not exist.")
        }
      }

      override def updateInfo(id: Int, firstName: Option[String], lastName: Option[String], username: Option[String], email: Option[String]): F[DbUnit] =
        Monad[F].pure {
          val userOpt = db.zipWithIndex.find(_._1.id == id)
          userOpt match {
            case Some((user, idx)) =>
              val fnCh = firstName.map(n => user.copy(firstName = Some(n))).getOrElse(user)
              val lnCh = lastName.map(n => fnCh.copy(lastName = Some(n))).getOrElse(fnCh)
              val usCh = username.map { n =>
                if(db.map(_.username).contains(n)) DbResult.mistake[User](s"User with username '$n' already exists.")
                else DbResult.of[User](lnCh.copy(username = n))
              }.getOrElse(DbResult.of(lnCh))
              val emCh: DbResult[User] = usCh.flatMap { user =>
                email.map { em =>
                  if(db.map(_.email).contains(em)) DbResult.mistake[User](s"User with email '$em' already exists.")
                  else DbResult.of[User](lnCh.copy(email = em))
                }.getOrElse(DbResult.of(user))
              }
              emCh.foreach(db(idx) = _)
              emCh.map(_ => ())
            case None => DbResult.mistake[Unit](s"User with id $id does not exist.")
          }
        }

      override def updateFirstName(id: Int, firstName: String): F[DbUnit] =
        updateInfo(id, Some(firstName), None, None, None)

      override def updateLastName(id: Int, lastName: String): F[DbUnit] =
        updateInfo(id, None, Some(lastName), None, None)

      override def updateUsername(id: Int, username: String): F[DbUnit] =
        updateInfo(id, None, None, Some(username), None)

      override def updateEmail(id: Int, email: String): F[DbUnit] =
        updateInfo(id, None, None, None, Some(email))
    }
}
