package ru.skelantros.easymacher.auth

import cats.data.{Kleisli, OptionT}
import cats.effect.kernel.Concurrent
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Cookie
import org.http4s.server.AuthMiddleware
import org.http4s._
import org.reactormonk.{CryptoBits, PrivateKey}
import ru.skelantros.easymacher.db.UserDb._
import ru.skelantros.easymacher.db.{DbError, Mistake, Thr}
import ru.skelantros.easymacher.entities.User
import ru.skelantros.easymacher.utils.StatusMessages

class CryptokeyAuth[F[_] : Concurrent](implicit db: Select[F]) {
  import CryptokeyAuth._
  private val dsl = new Http4sDsl[F] {}
  import dsl._

  def apply(service: AuthedRoutes[User, F]): HttpRoutes[F] = middleware(service)

  val key = PrivateKey(scala.io.Codec.toUTF8(scala.util.Random.alphanumeric.take(20).mkString("")))
  val crypto = CryptoBits(key)
  val clock = java.time.Clock.systemUTC


  private def generateToken(user: User): String =
    crypto.signToken(user.id.toString, clock.millis.toString)

  def dbErrorToStr(error: DbError): String = error match {
    case Mistake(msg) => msg
    case Thr(t) => t.getMessage
  }

  def verifyLogin(username: String, password: String): F[Either[String, User]] =
    db.userByUsername(username).map {
      case Right(user) =>
        if(user.password == password) Right(user)
        else Left(StatusMessages.invalidPassword)
      case Left(error) => Left(dbErrorToStr(error))
    }

  def retrieveUser: Kleisli[F, Int, User] = Kleisli(id => db.userById(id).map {
    case Right(user) => user
    case Left(error) => throw new Exception(dbErrorToStr(error))
  })

  val authUser: Kleisli[F, Request[F], Either[String, User]] = Kleisli { request =>
    val message = for {
      header <- request.headers.get[Cookie].toRight("Cookie parsing error")
      cookie <- header.values.toList.find(_.name == cookieName).toRight("Couldn't find the auth cookie.")
      token <- crypto.validateSignedToken(cookie.content).toRight("Cookie invalid")
      message <- Either.catchOnly[NumberFormatException](token.toInt).leftMap(_.toString)
    } yield message
    message.traverse(retrieveUser.run)
  }

  val onFailure: AuthedRoutes[String, F] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))
  val middleware: AuthMiddleware[F, User] = AuthMiddleware(authUser, onFailure)

  val loginService: HttpRoutes[F] = {
    case class NamePassword(username: String, password: String)
    implicit val decoder: EntityDecoder[F, NamePassword] = jsonOf

    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        for {
          namePassword <- req.as[NamePassword]
          NamePassword(name, password) = namePassword
          verifyRes <- verifyLogin(name, password)
          resp <- verifyRes match {
            case Right(user) =>
              val token = generateToken(user)
              Ok("Logged in!").map(_.addCookie(ResponseCookie(cookieName, token)))
            case Left(msg) =>
              Forbidden(msg)
          }
        } yield resp
    }
  }
}

object CryptokeyAuth {
  val cookieName: String = "authcookie"
}