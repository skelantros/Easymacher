package ru.skelantros.easymacher.auth

import cats.Monad
import cats.data.Kleisli
import cats.effect.kernel.Concurrent
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.implicits._
import ru.skelantros.easymacher.entities.User

object AuthLifter {
  def apply[F[_]](service: User => HttpRoutes[F]): UserRoutes[F] = Kleisli { authedReq =>
    val user = authedReq.context
    val req = authedReq.req
    service(user)(req)
  }

  def apply[F[_] : Monad](services: (User => HttpRoutes[F])*): UserRoutes[F] =
    services.map(AuthLifter(_)).reduce(_ <+> _)

  def apply[F[_]](service: HttpRoutes[F]): UserRoutes[F] = Kleisli { authedReq =>
    service(authedReq.req)
  }

  def apply[F[_] : Monad](services: HttpRoutes[F]*): UserRoutes[F] =
    services.map(AuthLifter(_)).reduce(_ <+> _)
}
