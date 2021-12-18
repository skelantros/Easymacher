package ru.skelantros.easymacher.services

import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.auto._
import cats.implicits._
import ru.skelantros.easymacher.db.DbResult
import ru.skelantros.easymacher.db.UserDb._
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.StatusMessages

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
    processDbDef(db.updateInfo(user.id, firstName, lastName, username))(userLight)
  }

  private implicit val lightEncoder: EntityEncoder[F, UserLight] = dropJsonEnc
  private implicit val seqLightEncoder: EntityEncoder[F, Seq[UserLight]] = dropJsonEnc
  private implicit val optLightEncoder: EntityEncoder[F, Option[UserLight]] = dropJsonEnc

  def selectServices(implicit db: Select[F]): HttpRoutes[F] =
    all <+> allByRole <+> byId
  def updateServices(user: User)(implicit db: Update[F]): HttpRoutes[F] =
    updateInfo(user)
  def selectOffsetServices(implicit db: SelectOffset[F]): HttpRoutes[F] =
    allOffset <+> allByRoleOffset

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

  def currentUser(user: User)(implicit db: Select[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "profile" =>
      Ok(userLight(user))
  }

  def updateInfo(user: User)(implicit db: Update[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "update-info" =>
      req.as[UpdInfo].flatMap(updateUserInfo(user, _))
  }

  def editUser(u: User)(implicit dbSel: Select[F], dbUpd: Update[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "user" / IntVar(id) / "edit" =>
      for {
        body <- req.as[UpdInfo]
        dbUser <- dbSel.userById(id)
        resp <- editAction(dbUser, u)(updateUserInfo(_, body))
      } yield resp
  }

  def removeUser(u: User)(implicit dbSel: Select[F], dbRem: Remove[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root / "user" / IntVar(id) / "remove" =>
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