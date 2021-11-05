package ru.skelantros.easymacher.services

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s._
import org.scalatest.flatspec.AnyFlatSpec
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.services.UserServices.UserLight
import ru.skelantros.easymacher.{CommonSpec, DbMocks}

class UserServicesSpec extends AnyFlatSpec with CommonSpec {
  val usersSample = Seq(
    User(1, "skelantros", "skelantros@easymacher.ru", Role.Admin, true, Some("Alex"), Some("Egorowski")),
    User(2, "adefful", "ad3fful@easymacher.ru", Role.User, true, Some("Alex"), None),
    User(3, "g03th3", "g03th3@klassik.de", Role.User, true, None, None),
    User(4, "damned", "damned@mail.ru", Role.User, false, None, None)
  )
  val services = new UserServices[IO]

  implicit val lightEncoder: EntityEncoder[IO, UserLight] = jsonEncoderOf
  implicit val seqLightEncoder: EntityEncoder[IO, Seq[UserLight]] = jsonEncoderOf
  implicit val optLightEncoder: EntityEncoder[IO, Option[UserLight]] = jsonEncoderOf
  implicit val lightDecoder: EntityDecoder[IO, UserLight] = jsonOf
  implicit val seqLightDecoder: EntityDecoder[IO, Seq[UserLight]] = jsonOf
  implicit val optLightDecoder: EntityDecoder[IO, Option[UserLight]] = jsonOf

  val allUsersReq = Request[IO](method = Method.GET, uri = uri"/users")
  val allUsersByRoleReq = Request[IO](method = Method.GET, uri = uri"/users?role=user")
  val allAdminsByRoleReq = Request[IO](method = Method.GET, uri = uri"/users?role=admin")
  val allTrashByRoleReq = Request[IO](method = Method.GET, uri = uri"/users?role=trash")

  "An empty User Database" should "not return any users" in {
    implicit val db = DbMocks.userDbSelect[IO](Seq())
    val actualResp = services.all.orNotFound.run(allUsersReq)
    check(actualResp, Status.Ok, Some(Seq.empty[UserLight]))
  }

  it should "not return any users by role" in {
    implicit val db = DbMocks.userDbSelect[IO](Seq())
    val actualResp = services.all.orNotFound.run(allUsersByRoleReq)
    check(actualResp, Status.Ok, Some(Seq.empty[UserLight]))
  }

  "all" should "return all users correctly" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val actualResp = services.all.orNotFound.run(allUsersReq)
    check(actualResp, Status.Ok, Some(usersSample.map(UserServices.userLight)))
  }

  "allByRole" should "return all users by role correctly" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val actualResp = services.allByRole.orNotFound.run(allUsersByRoleReq)
    check(actualResp, Status.Ok,
      Some(usersSample.filter(_.role == Role.User).map(UserServices.userLight))
    )
  }

  it should "return all admins by role correctly" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val actualResp = services.allByRole.orNotFound.run(allAdminsByRoleReq)
    check(actualResp, Status.Ok,
      Some(usersSample.filter(_.role == Role.Admin).map(UserServices.userLight))
    )
  }

  it should "catch incorrect role correctly" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val actualResp = services.allByRole.orNotFound.run(allTrashByRoleReq)
    check(actualResp, Status.BadRequest,
      Some(s"Role 'trash' does not exist.")
    )
  }

  "byId" should "return existing user" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri=uri"/user?id=1")
    val actualResp = services.byId.orNotFound.run(req)
    check(actualResp, Status.Ok,
      Some(UserServices.userLight(usersSample.head))
    )
  }

  it should "not return non-existing user" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri=uri"/user?id=5")
    val actualResp = services.byId.orNotFound.run(req)
    check(actualResp, Status.BadRequest,
      Some("User with id 5 does not exist.")
    )
  }

  "byUsername" should "return existing user" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri=uri"/user?username=skelantros")
    val actualResp = services.byUsername.orNotFound.run(req)
    check(actualResp, Status.Ok,
      Some(UserServices.userLight(usersSample.head))
    )
  }

  it should "not return non-existing user" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri=uri"/user?username=whoisthat")
    val actualResp = services.byUsername.orNotFound.run(req)
    check(actualResp, Status.BadRequest,
      Some("User with username 'whoisthat' does not exist.")
    )
  }
}
