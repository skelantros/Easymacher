package ru.skelantros.easymacher.auth

import org.http4s.HttpRoutes

trait AuthWare[F[_]] {
  def apply(service: UserRoutes[F]): HttpRoutes[F]
}
