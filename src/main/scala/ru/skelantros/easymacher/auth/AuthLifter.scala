package ru.skelantros.easymacher.auth

import cats.data.Kleisli
import org.http4s.HttpRoutes
import ru.skelantros.easymacher.entities.User

// TODO добавить функции для преобразования нескольких сервисов разом
object AuthLifter {
  def apply[F[_]](service: User => HttpRoutes[F]): UserRoutes[F] = Kleisli { authedReq =>
    val user = authedReq.context
    val req = authedReq.req
    service(user)(req)
  }

  def apply[F[_]](service: HttpRoutes[F]): UserRoutes[F] = Kleisli { authedReq =>
    service(authedReq.req)
  }
}
