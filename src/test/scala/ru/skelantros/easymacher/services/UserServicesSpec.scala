package ru.skelantros.easymacher.services

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import ru.skelantros.easymacher.db.DbResult
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.services.UserServices.UserLight
import ru.skelantros.easymacher.utils.Email
import ru.skelantros.easymacher.{CommonSpec, DbMocks}

class UserServicesSpec extends AnyFlatSpec with CommonSpec {
  val usersSample = Seq(
    User(1, "skelantros", Email("skelantros@easymacher.ru").get, "23052001", Role.Admin, true, Some("Alex"), Some("Egorowski")),
    User(2, "adefful", Email("ad3fful@easymacher.ru").get, "1234", Role.User, true, Some("Alex"), None),
    User(3, "g03th3", Email("g03th3@klassik.de").get, "5678", Role.User, true, None, None),
    User(4, "damned", Email("damned@mail.ru").get, "xd", Role.User, false, None, None)
  )


  val skelantrosJson = Json.obj(
    "id" := 1,
    "username" := "skelantros",
    "role" := "admin",
    "firstName" := "Alex",
    "lastName" := "Egorowski"
  )

  val services = new UserServices[IO]

  implicit val seqLightEncoder: EntityEncoder[IO, Seq[UserLight]] = jsonEncoderOf
  implicit val optLightEncoder: EntityEncoder[IO, Option[UserLight]] = jsonEncoderOf
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

  it should "ignore input role case" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri = uri"/users?role=AdMiN")
    val actualResp = services.allByRole.orNotFound.run(req)
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
      Some(skelantrosJson)
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
      Some(skelantrosJson)
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

  "updatePassword" should "update password for existing users correctly" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val body = Json.obj(
      "old" := "23052001",
      "new" := "2305"
    )
    val req = Request[IO](method = Method.POST, uri=uri"/update-password").withEntity(body)
    val actualResp = services.updatePassword(usersSample.head).orNotFound.run(req)
    check(actualResp, Status.Ok, Option(()))
    // check if user has been changed in DB
    db.userById(1).unsafeRunSync() shouldBe DbResult.of(usersSample.head.copy(password = "2305"))
  }

  it should "not accept requests with wrong password" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val body = Json.obj(
      "old" := "2305201",
      "new" := "2305"
    )
    val req = Request[IO](method = Method.POST, uri=uri"/update-password").withEntity(body)
    val actualResp = services.updatePassword(usersSample.head).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Option("Wrong password."))
    // check if user has not been changed in DB
    db.userById(1).unsafeRunSync() shouldBe DbResult.of(usersSample.head)
  }

  "updateInfo" should "correctly change 'simple' parameters" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val body = Json.obj(
      "firstName" := "Alexander"
    )
    val req = Request[IO](method = Method.POST, uri=uri"/update-info").withEntity(body)
    val actualResp = services.updateInfo(usersSample.head).orNotFound.run(req)
    check(actualResp, Status.Ok, Option(()))
    // check if user has been changed in DB
    db.userById(1).unsafeRunSync() shouldBe DbResult.of(usersSample.head.copy(firstName = Some("Alexander")))
  }

  it should "correctly change 'complex' parameters" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val body = Json.obj {
      "username" := "Skel"
    }
    val req = Request[IO](method = Method.POST, uri=uri"/update-info").withEntity(body)
    val actualResp = services.updateInfo(usersSample.head).orNotFound.run(req)
    check(actualResp, Status.Ok, Option(()))
    // check if user has been changed in DB
    db.userById(1).unsafeRunSync() shouldBe DbResult.of(usersSample.head.copy(username = "Skel"))
  }

  it should "change parameters in transaction (if something goes wrong, nothing changes)" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val body = Json.obj(
      "username" := "adefful",
      "firstName" := "Alexander"
    )
    val req = Request[IO](method = Method.POST, uri=uri"/update-info").withEntity(body)
    val actualResp = services.updateInfo(usersSample.head).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Option("User with username 'adefful' already exists."))
    // check if nothing has been changed in DB
    db.userById(1).unsafeRunSync() shouldBe DbResult.of(usersSample.head)
  }
}
