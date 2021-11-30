package ru.skelantros.easymacher.doobieimpl.user

import java.util.UUID

import cats.Monad
import cats.implicits._
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.{DbResult, DbUnit}
import ru.skelantros.easymacher.db.UserDb._
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email
import ru.skelantros.easymacher.doobieimpl._
import ru.skelantros.easymacher.utils.StatusMessages._
import UserQueries._
import cats.effect.kernel.Async
import cats.effect.{MonadCancelThrow, Sync}
import doobie.implicits.toConnectionIOOps

class UserDoobie[F[_] : Async](implicit val xa: Transactor[F]) extends Select[F] with Update[F] with Register[F] {
  private val note = (n: Note) => n.toUser
  private def seq[W[_] : Monad]: W[Note] => W[User] = (n: W[Note]) => n.map(note)

  override def allUsers: F[DbResult[Seq[User]]] = processSelect(selectAll)(seq)
  override def usersByRole(role: Role): F[DbResult[Seq[User]]] = processSelect(selectByAdmin(role == Role.Admin))(seq)

  override def userById(id: Int): F[DbResult[User]] =
    processOptSelect(selectById(id))(_.toUser, noUserById(id))
  override def userByUsername(username: String): F[DbResult[User]] =
    processOptSelect(selectByUsername(username))(_.toUser, noUserByUsername(username))
  override def userByEmail(email: Email): F[DbResult[User]] =
    processOptSelect(selectByEmail(email.asString))(_.toUser, noUserByEmail(email))

  override def updatePassword(id: Int, oldPassword: String, newPassword: String): F[DbUnit] =
    

  override def updateInfo(id: Int, firstName: Option[String], lastName: Option[String], username: Option[String], email: Option[Email]): F[DbUnit] =
    processSelect(update(id, email.map(_.asString), username, firstName, lastName))(_ => ())

  private def generateToken: F[String] = Sync[F].blocking(UUID.randomUUID().toString.filter(_ != '-'))

  // TODO учесть, что uuid может сгенерироваться не уникальным образом
  override def createUser(username: String, password: String, email: Email, role: Role): F[DbUnit] = {
    val sqlRequest = for {
      byUsername <- selectByUsername(username)
      byEmail <- selectByEmail(email.asString)
    } yield (byUsername, byEmail)

    for {
      findReq <- sqlRequest.transact(xa)
      res <- findReq match {
        case (Some(_), _) => DbResult.mistake[Unit](userByUsernameExists(username)).pure[F]
        case (_, Some(_)) => DbResult.mistake[Unit](userByEmailExists(email)).pure[F]
        case _ => for {
          token <- generateToken
          res2 <- processSelect(create(username, password, email.asString, if(role == Role.Admin) true else false, token))(_ => ())
        } yield res2
      }
    } yield res
  }

  override def activateUser(uuid: String): F[DbUnit] =
    for {
      userOpt <- selectByToken(uuid).transact(xa)
      res <- userOpt.map(_.toUser) match {
        case None => DbUnit.mistake(noUserByToken(uuid)).pure[F]
        case Some(user) if user.isActivated => DbUnit.mistake(userAlreadyActivated(uuid)).pure[F]
        case Some(user) => processSelect(activate(uuid))(_ => ())
      }
    } yield res
}
