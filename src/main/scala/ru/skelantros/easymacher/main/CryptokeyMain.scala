package ru.skelantros.easymacher.main

import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import org.http4s.HttpApp
import ru.skelantros.easymacher.doobieimpl.user.UserDoobie
import ru.skelantros.easymacher.services.UserServices
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s._
import cats.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import ru.skelantros.easymacher.auth.{AuthLifter, CryptokeyAuth, UserRoutes}
import ru.skelantros.easymacher.utils.TransactorImpl

import scala.concurrent.ExecutionContext.global

object CryptokeyMain extends IOApp {
  implicit val transactor = TransactorImpl[IO]
  implicit val userDb = new UserDoobie[IO]
  val userServices = new UserServices[IO]

  val auth = new CryptokeyAuth[IO]

  val unauthServices: HttpRoutes[IO] =
    userServices.registerServices <+> auth.loginService

  val authNonIdServices: UserRoutes[IO] = AuthLifter(userServices.selectServices)

  val authIdServices: UserRoutes[IO] = AuthLifter(userServices.updateServices(_))


  val app: HttpApp[IO] =
    (unauthServices <+> auth(authNonIdServices) <+> auth(authIdServices)).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
