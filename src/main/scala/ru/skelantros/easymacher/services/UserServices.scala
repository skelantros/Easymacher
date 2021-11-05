package ru.skelantros.easymacher.services

import cats.Monad
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.auto._
import cats.implicits._
import ru.skelantros.easymacher.db.UserDb.{Select, SelectOffset}
import ru.skelantros.easymacher.entities.{Role, User}

class UserServices[F[_] : Monad] {
  val dsl = new Http4sDsl[F] {}
  import dsl._
  import UserServices._

  private implicit val lightEncoder: EntityEncoder[F, UserLight] = jsonEncoderOf
  private implicit val seqLightEncoder: EntityEncoder[F, Seq[UserLight]] = jsonEncoderOf
  private implicit val optLightEncoder: EntityEncoder[F, Option[UserLight]] = jsonEncoderOf

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
}

object UserServices {
  case class UserLight(id: Int, username: String, role: Role,
                       firstName: Option[String], lastName: Option[String])
  def userLight(user: User) =
    UserLight(user.id, user.username, user.role, user.firstName, user.lastName)
}