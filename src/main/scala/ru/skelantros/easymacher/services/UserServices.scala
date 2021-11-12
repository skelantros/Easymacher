package ru.skelantros.easymacher.services

import cats.Monad
import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.auto._
import cats.implicits._
import ru.skelantros.easymacher.db.UserDb._
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email

class UserServices[F[_] : Concurrent] {
  val dsl = new Http4sDsl[F] {}
  import dsl._
  import UserServices._

  private implicit val lightEncoder: EntityEncoder[F, UserLight] = dropJsonEnc
  private implicit val seqLightEncoder: EntityEncoder[F, Seq[UserLight]] = dropJsonEnc
  private implicit val optLightEncoder: EntityEncoder[F, Option[UserLight]] = dropJsonEnc

  def all(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" =>
      processDbDef(db.allUsers)(_ map userLight)
  }

  def allOffset(implicit db: SelectOffset[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" :? OffsetParam(offset) :? LimitParam(limit) =>
      processDbDef(db.allUsers(offset, limit))(_ map userLight)
  }

  def allByRole(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" :? RoleParam(role) =>
      parseRole(role).fold(
        BadRequest(s"Role '$role' does not exist.")
      )(r => processDbDef(db.usersByRole(r))(_ map userLight))
  }

  def allByRoleOffset(implicit db: SelectOffset[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" :? RoleParam(role) :? OffsetParam(offset) :? LimitParam(limit) =>
      parseRole(role).fold(
        BadRequest(s"Role '$role' does not exist.")
      )(r => processDbDef(db.usersByRole(r)(offset, limit))(_ map userLight))
  }

  def byId(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" :? IdParam(id) =>
      processDbDef(db.userById(id))(userLight)
  }

  def byUsername(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" :? UsernameParam(username) =>
      processDbDef(db.userByUsername(username))(userLight)
  }

  def byEmail(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" :? EmailParam(email) =>
      Email(email).fold(
        BadRequest("Incorrect email")
      )(em => processDbDef(db.userByEmail(em))(userLight))
  }

  def updatePassword(user: User)(implicit db: Update[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "update-password" =>
      val body = req.as[UpdPassword]
      body.flatMap { updPass =>
        processDbDef(db.updatePassword(user.id, updPass.old, updPass.`new`))(identity)
      }
  }

  def updateInfo(user: User)(implicit db: Update[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "update-info" =>
      val body = req.as[UpdInfo]
      body.flatMap { updInfo =>
        val UpdInfo(emailOpt, username, firstName, lastName) = updInfo
        emailOpt match {
          case None => processDbDef(db.updateInfo(user.id, firstName, lastName, username, None))(identity)
          case Some(emStr) => Email(emStr).fold(
            BadRequest("Incorrect email")
          )(email => processDbDef(db.updateInfo(user.id, firstName, lastName, username, Some(email)))(identity))
        }
      }
  }

  def createUser(implicit db: Register[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "register" =>
      val body = req.as[CreateUser]
      body.flatMap { createUser =>
        val CreateUser(username, password, email) = createUser
        processDbEmail(email, db.createUser(username, password, _, Role.User))(identity)
      }
  }

  def activateUser(implicit db: Register[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "activate" :? ActivateTokenParam(token) =>
      processDbDef(db.activateUser(token))(identity)
  }
}

object UserServices {
  case class UserLight(id: Int, username: String, role: String,
                       firstName: Option[String], lastName: Option[String]) {
    def this(id: Int, username: String, role: Role, firstName: Option[String], lastName: Option[String]) = {
      this(id, username, role.asString, firstName, lastName)
    }
  }
  object UserLight {
    def apply(id: Int, username: String, role: Role,
              firstName: Option[String], lastName: Option[String]): UserLight =
      new UserLight(id, username, role, firstName, lastName)

    implicit def encoder[F[_]]: EntityEncoder[F, UserLight] = dropJsonEnc
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, UserLight] = jsonOf
  }

  def userLight(user: User) =
    UserLight(user.id, user.username, user.role, user.firstName, user.lastName)

  case class UpdPassword(old: String, `new`: String)
  object UpdPassword {
    implicit def encoder[F[_]]: EntityEncoder[F, UpdPassword] = dropJsonEnc
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, UpdPassword] = jsonOf
  }

  case class UpdInfo(email: Option[String], username: Option[String], firstName: Option[String], lastName: Option[String])
  object UpdInfo {
    implicit def encoder[F[_]]: EntityEncoder[F, UpdInfo] = dropJsonEnc
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, UpdInfo] = jsonOf
  }

  case class CreateUser(username: String, password: String, email: String)
  object CreateUser {
    implicit def encoder[F[_]]: EntityEncoder[F, CreateUser] = dropJsonEnc
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, CreateUser] = jsonOf
  }
}