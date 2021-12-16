package ru.skelantros.easymacher.main

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig, Logger}
import ru.skelantros.easymacher.auth.{Auth0Auth, AuthLifter, CryptokeyAuth, UserRoutes}
import ru.skelantros.easymacher.doobieimpl.flashcard.FlashCardDoobie
import ru.skelantros.easymacher.doobieimpl.group.WordGroupDoobie
import ru.skelantros.easymacher.doobieimpl.user.{Auth0Doobie, UserDoobie}
import ru.skelantros.easymacher.doobieimpl.word.WordDoobie
import ru.skelantros.easymacher.services.{FlashCardServices, UserServices, WordGroupServices, WordServices}
import ru.skelantros.easymacher.utils.TransactorImpl
import cats.implicits._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.global

object Auth0Main extends IOApp {
  implicit val transactor = TransactorImpl[IO]
  implicit val userDb = new UserDoobie[IO]
  implicit val wordsDb = new WordDoobie[IO]
  implicit val wordGroupsDb = new WordGroupDoobie[IO]
  implicit val flashDb = new FlashCardDoobie[IO]
  implicit val authDb = new Auth0Doobie[IO]

  val userServices = new UserServices[IO]
  val wordServices = new WordServices[IO]
  val groupServices = new WordGroupServices[IO]
  val flashServices = new FlashCardServices[IO]

  val auth = new Auth0Auth[IO](Auth0Auth.Config("skelantros-test.eu.auth0.com", "http://localhost:8080"))

  val authNonIdServices: UserRoutes[IO] = AuthLifter(
    userServices.selectServices <+>
      wordServices.selectServices
  )

  val authIdServices: UserRoutes[IO] = AuthLifter(
    userServices.updateServices(_),
    userServices.editUser(_),
    userServices.currentUser(_),
    wordServices.selectUserServices,
    wordServices.addWord(_),
    wordServices.removeServices,
    groupServices.allServices,
    flashServices.allServices
  )

  val allServices = auth(authNonIdServices) <+> auth(authIdServices)

  val corsConfig = CORSConfig.default
    .withAnyOrigin(false)
    .withAllowedOrigins(Set("http://localhost:3000"))
    .withAnyMethod(true)
    .withAllowCredentials(true)

  val cors: HttpRoutes[IO] = CORS(allServices, corsConfig)

  val app: HttpApp[IO] = cors.orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(Logger.httpApp(true, true)(app))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
