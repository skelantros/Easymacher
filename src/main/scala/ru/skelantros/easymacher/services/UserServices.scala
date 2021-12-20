package ru.skelantros.easymacher.services

import cats.Monad
import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.auto._
import cats.implicits._
import ru.skelantros.easymacher.db.DbResult
import ru.skelantros.easymacher.db.UserDb._
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.{Email, StatusMessages}

class UserServices[F[_] : Concurrent] {
  val dsl = new Http4sDsl[F] {}
  import dsl._
  import UserServices._

  private def editAction(dbUser: DbResult[User], u: User)
                        (ifAccess: User => F[Response[F]]): F[Response[F]] =
    dbUser match {
      case Right(user) if user.id == u.id || u.role == Role.Admin =>
        ifAccess(user)
      case Right(user) =>
        Forbidden(StatusMessages.cannotEditUser(user.id))
      case Left(error) =>
        responseWithError[F](error)
    }

  private def updateUserInfo(user: User, updInfo: UpdInfo)(implicit db: Update[F]): F[Response[F]] = {
    val UpdInfo(emailOpt, username, firstName, lastName) = updInfo.insensitive
    emailOpt match {
      case None => processDbDef(db.updateInfo(user.id, firstName, lastName, username, None))(userLight)
      case Some(emStr) => Email(emStr).fold(
        BadRequest("Incorrect email")
      )(email => processDbDef(db.updateInfo(user.id, firstName, lastName, username, Some(email)))(userLight))
    }
  }

  private implicit val lightEncoder: EntityEncoder[F, UserLight] = dropJsonEnc
  private implicit val seqLightEncoder: EntityEncoder[F, Seq[UserLight]] = dropJsonEnc
  private implicit val optLightEncoder: EntityEncoder[F, Option[UserLight]] = dropJsonEnc

  def selectServices(implicit db: Select[F]): HttpRoutes[F] =
    all <+> allByRole <+> byId <+> byUsername <+> byEmail
  def updateServices(user: User)(implicit db: Update[F]): HttpRoutes[F] =
    updatePassword(user) <+> updateInfo(user)
  def registerServices(implicit db: Register[F]): HttpRoutes[F] =
    createUser <+> activateUser
  def selectOffsetServices(implicit db: SelectOffset[F]): HttpRoutes[F] =
    allOffset <+> allByRoleOffset

  def all(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" / "all" =>
      processDbDef(db.allUsers)(_ map userLight)
  }

  def allOffset(implicit db: SelectOffset[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "users" / "all" :? OffsetParam(offset) :? LimitParam(limit) =>
      processDbDef(db.allUsers(offset, limit))(_ map userLight)
  }

  def allByRole(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" / "all" :? RoleParam(role) =>
      parseRole(role).fold(
        BadRequest(s"Role '$role' does not exist.")
      )(r => processDbDef(db.usersByRole(r))(_ map userLight))
  }

  def allByRoleOffset(implicit db: SelectOffset[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" / "all" :? RoleParam(role) :? OffsetParam(offset) :? LimitParam(limit) =>
      parseRole(role).fold(
        BadRequest(s"Role '$role' does not exist.")
      )(r => processDbDef(db.usersByRole(r)(offset, limit))(_ map userLight))
  }

  def byId(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" /  IntVar(id) =>
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

  def currentUser(user: User)(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "profile" =>
      Ok(userLight(user))
  }

  def updatePassword(user: User)(implicit db: Update[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PATCH -> Root / "profile" / "update-password" =>
      val body = req.as[UpdPassword]
      body.flatMap { updPass =>
        processDbDef(db.updatePassword(user.id, updPass.old, updPass.`new`))(userLight)
      }
  }

  def updateInfo(user: User)(implicit db: Update[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PATCH -> Root / "profile" / "update-info" =>
      req.as[UpdInfo].flatMap(updateUserInfo(user, _))
  }

  def createUser(implicit db: Register[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "register" =>
      val body = req.as[CreateUser]
      body.flatMap {
        case CreateUser(username, password, email) =>
          processDbEmail(email, db.createUser(username, password, _, Role.User))(identity)
      }
  }

  def activateUser(implicit db: Register[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case PATCH -> Root / "activate" :? ActivateTokenParam(token) =>
      processDbDef(db.activateUser(token))(identity)
  }

  def editUser(u: User)(implicit dbSel: Select[F], dbUpd: Update[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / "user" / IntVar(id) =>
      for {
        body <- req.as[UpdInfo]
        dbUser <- dbSel.userById(id)
        resp <- editAction(dbUser, u)(updateUserInfo(_, body))
      } yield resp
  }

  def removeUser(u: User)(implicit dbSel: Select[F], dbRem: Remove[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / "user" / IntVar(id)  =>
      for {
        toRemoveDb <- dbSel.userById(id)
        resp <- editAction(toRemoveDb, u)(toRemove => processDbDef(dbRem.removeUser(toRemove.id))(identity))
      } yield resp
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

  case class UpdInfo(email: Option[String], username: Option[String], firstName: Option[String], lastName: Option[String]) {
    def insensitive: UpdInfo = UpdInfo(email.map(_.toLowerCase), username.map(_.toLowerCase), firstName, lastName)
  }
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