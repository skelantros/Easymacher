package ru.skelantros.easymacher.main

import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import org.http4s.HttpApp
import ru.skelantros.easymacher.doobieimpl.user.UserDoobie
import ru.skelantros.easymacher.services.{UserServices, WordGroupServices, WordServices}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s._
import cats.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import ru.skelantros.easymacher.auth.{AuthLifter, CryptokeyAuth, UserRoutes}
import ru.skelantros.easymacher.doobieimpl.group.WordGroupDoobie
import ru.skelantros.easymacher.doobieimpl.word.WordDoobie
import ru.skelantros.easymacher.utils.TransactorImpl

import scala.concurrent.ExecutionContext.global

object CryptokeyMain extends IOApp {
  implicit val transactor = TransactorImpl[IO]
  implicit val userDb = new UserDoobie[IO]
  implicit val wordsDb = new WordDoobie[IO]
  implicit val wordGroupsDb = new WordGroupDoobie[IO]

  val userServices = new UserServices[IO]
  val wordServices = new WordServices[IO]
  val groupServices = new WordGroupServices[IO]

  val auth = new CryptokeyAuth[IO]

  val unauthServices: HttpRoutes[IO] =
    userServices.registerServices <+> auth.loginService

  val authNonIdServices: UserRoutes[IO] = AuthLifter(
    userServices.selectServices <+>
    wordServices.selectServices
  )

  val authIdServices: UserRoutes[IO] = AuthLifter(
    userServices.updateServices(_),
    userServices.currentUser(_),
    wordServices.selectUserServices,
    wordServices.addWord(_),
    wordServices.removeServices,
    groupServices.allServices
  )

  val allServices = unauthServices <+> auth(authNonIdServices) <+> auth(authIdServices)

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
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}