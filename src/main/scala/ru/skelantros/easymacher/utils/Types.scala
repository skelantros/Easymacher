package ru.skelantros.easymacher.utils

import org.http4s.AuthedRoutes
import ru.skelantros.easymacher.entities.User

object Types {
  type UserRoutes[F[_]] = AuthedRoutes[User, F]
  type OrThrowable[A] = Either[Throwable, A]
}
