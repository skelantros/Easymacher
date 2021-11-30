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
import ru.skelantros.easymacher.utils.TransactorImpl

import scala.concurrent.ExecutionContext.global

object LocalDbMain extends IOApp {
  implicit val transactor = TransactorImpl[IO]
  implicit val userDb = new UserDoobie[IO]
  val userServices = new UserServices[IO]

  val app: HttpApp[IO] =
    (userServices.activateUser <+> userServices.createUser <+>
//    userServices.updateInfo <+> userServices.updatePassword <+>
    userServices.byUsername <+> userServices.byId <+> userServices.byEmail <+>
    userServices.allByRole <+> userServices.all).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
