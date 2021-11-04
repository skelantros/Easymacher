package ru.skelantros.easymacher

import cats.Monad
import org.http4s.{EntityEncoder, Response}
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import ru.skelantros.easymacher.db.DbResult
import ru.skelantros.easymacher.entities.Role

package object services {
  type RespF[F[_]] = F[Response[F]]

  // TODO написать логику логирования
  def logThrowable[F[_] : Monad](t: Throwable): F[Unit] = Monad[F].unit

  object OffsetParam extends QueryParamDecoderMatcher[Int]("offset")
  object LimitParam extends QueryParamDecoderMatcher[Int]("limit")
  object IdParam extends QueryParamDecoderMatcher[Int]("id")
  object UsernameParam extends QueryParamDecoderMatcher[String]("username")
  object RoleParam extends QueryParamDecoderMatcher[String]("role")
  def parseRole(str: String): Option[Role] = str.toLowerCase match {
    case "user" => Some(Role.User)
    case "admin" => Some(Role.Admin)
    case _ => None
  }

  def processDb[F[_] : Monad, A](res: =>F[DbResult[A]])
                                (success: A => RespF[F],
                                 mistake: String => RespF[F],
                                 thr: Throwable => RespF[F]): RespF[F] =
    for {
      dbRes <- res
      resp <- dbRes match {
        case Right(a) => success(a)
        case Left(error) => error.map(mistake, thr)
      }
    } yield resp

  def processDbDef[F[_] : Monad, A, B : ({type E[V] = EntityEncoder[F, V]})#E](res: =>F[DbResult[A]])(f: A => B): RespF[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    processDb(res)(
      a => Ok(f(a)),
      msg => BadRequest(msg),
      t => logThrowable[F](t) >> InternalServerError()
    )
  }
}
