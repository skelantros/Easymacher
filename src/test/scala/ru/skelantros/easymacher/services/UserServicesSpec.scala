package ru.skelantros.easymacher.services

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import ru.skelantros.easymacher.db.{DbResult, UserDb}
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.services.UserServices.UserLight
import ru.skelantros.easymacher.utils.Email
import ru.skelantros.easymacher.{CommonSpec, DbMocks}

class UserServicesSpec extends AnyFlatSpec with CommonSpec {
  val usersSample = Seq(
    User(1, "skelantros", Email("skelantros@easymacher.ru").get, "23052001", Role.Admin, true, "001", Some("Alex"), Some("Egorowski")),
    User(2, "adefful", Email("ad3fful@easymacher.ru").get, "1234", Role.User, true, "002", Some("Alex"), None),
    User(3, "g03th3", Email("g03th3@klassik.de").get, "5678", Role.User, true, "003", None, None),
    User(4, "damned", Email("damned@mail.ru").get, "xd", Role.User, false, "004", None, None)
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

  "byEmail" should "catch non-valid emails" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri=uri"/user?email=nonvalid@yandex..ru")
    val actualResp = services.byEmail.orNotFound.run(req)
    check(actualResp, Status.BadRequest,
      Some("Incorrect email")
    )
  }

  it should "return existing user" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri=uri"/user?email=skelantros@easymacher.ru")
    val actualResp = services.byEmail.orNotFound.run(req)
    check(actualResp, Status.Ok,
      Some(skelantrosJson)
    )
  }

  it should "not return non-existing user" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.GET, uri=uri"/user?email=skelll@easymacher.ru")
    val actualResp = services.byEmail.orNotFound.run(req)
    check(actualResp, Status.BadRequest,
      Some("User with email 'skelll@easymacher.ru' does not exist.")
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

  it should "catch non-valid emails" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val body = Json.obj(
      "email" := "skelll@yandex..ru"
    )
    val req = Request[IO](method = Method.POST, uri=uri"/update-info").withEntity(body)
    val actualResp = services.updateInfo(usersSample.head).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Option("Incorrect email"))
    // check if nothing has been changed in DB
    db.userById(1).unsafeRunSync() shouldBe DbResult.of(usersSample.head)
  }

  def createUserReq(json: Json, db: UserDb.Register[IO]): IO[Response[IO]] = {
    val req = Request[IO](method = Method.POST, uri=uri"/register").withEntity(json)
    val actualResp = services.createUser(db).orNotFound.run(req)
    actualResp
  }
  def createUserSampleReq(json: Json): (IO[Response[IO]], UserDb.Select[IO] with UserDb.Register[IO]) = {
    val db = DbMocks.userDbSelect[IO](usersSample)
    (createUserReq(json, db), db)
  }

  "createUser" should "create user with correct inputs (unique username & email, valid password & email)" in {
    val body = Json.obj(
      "username" := "alegor",
      "email" := "alegor@yandex.ru",
      "password" := "1234"
    )
    val (actualResp, db) = createUserSampleReq(body)
    check(actualResp, Status.Ok, Option(()))
    // check if user has been created
    val expectedUser = User(5, "alegor", Email("alegor@yandex.ru").get, "1234", Role.User, false, "005")
    db.userById(5).unsafeRunSync() shouldBe DbResult.of(expectedUser)
  }

  it should "not accept invalid emails" in {
    val body = Json.obj(
      "username" := "alegor",
      "email" := "alegor@yandex..ru",
      "password" := "1234"
    )
    val (actualResp, db) = createUserSampleReq(body)
    check(actualResp, Status.BadRequest, Option("Incorrect email"))
    // check if user has not been created
    db.userById(5).unsafeRunSync() shouldBe DbResult.mistake("User with id 5 does not exist.")
  }

  it should "not accept existing usernames" in {
    val body = Json.obj(
      "username" := "Skelantros",
      "email" := "alegor@yandex.ru",
      "password" := "1234"
    )
    val (actualResp, db) = createUserSampleReq(body)
    check(actualResp, Status.BadRequest, Option("User with username 'Skelantros' already exists."))
    // check if user has not been created
    db.userById(5).unsafeRunSync() shouldBe DbResult.mistake("User with id 5 does not exist.")
  }

  it should "not accept existing emails" in {
    val body = Json.obj(
      "username" := "alegor",
      "email" := "skeLantros@easymaCher.ru",
      "password" := "1234"
    )
    val (actualResp, db) = createUserSampleReq(body)
    check(actualResp, Status.BadRequest, Option("User with email 'skeLantros@easymaCher.ru' already exists."))
    // check if user has not been created
    db.userById(5).unsafeRunSync() shouldBe DbResult.mistake("User with id 5 does not exist.")
  }

  it should "not accept existing invalid passwords (empty in mock)" in {
    val body = Json.obj(
      "username" := "alegor",
      "email" := "alegor@yandex.ru",
      "password" := ""
    )
    val (actualResp, db) = createUserSampleReq(body)
    check(actualResp, Status.BadRequest, Option("Invalid password."))
    // check if user has not been created
    db.userById(5).unsafeRunSync() shouldBe DbResult.mistake("User with id 5 does not exist.")
  }

  "activateUser" should "activate users" in {
    implicit val db = DbMocks.userDbSelect[IO](usersSample)
    val req = Request[IO](method = Method.POST, uri=uri"/activate?token=004")
    val actualResp = services.activateUser.orNotFound.run(req)
    check(actualResp, Status.Ok, Option(()))
    // check if user has been activated
    db.userById(4).unsafeRunSync() shouldBe DbResult.of(usersSample(3).copy(isActivated = true))
  }
}
