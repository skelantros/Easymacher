package ru.skelantros.easymacher.auth.auth0

import cats.effect.{Async, IO}
import cats.effect.kernel.{Concurrent, Resource}
import org.http4s.{EntityDecoder, Headers, Method, Request, Uri}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization

import scala.concurrent.ExecutionContext.global

object Auth0Client {
  case class UserInfo(nickname: String, given_name: Option[String], family_name: Option[String])
  object UserInfo {
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, UserInfo] = jsonOf
  }

  def userInfoRequest[F[_] : Async](token: String, apiAddress: String)(client: Client[F]): F[UserInfo] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    val request = Request[F](
      method = Method.GET,
      uri = Uri.fromString(s"$apiAddress/userinfo").getOrElse(uri"/"),
      headers = Headers("Authorization" -> s"Bearer $token")
    )

    client.expect[UserInfo](request)
  }

  def makeRequest[F[_] : Async, A](req: Client[F] => F[A]): F[A] =
    BlazeClientBuilder[F](global).resource.use(req)
}
