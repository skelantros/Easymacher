package ru.skelantros.easymacher.utils

import org.http4s.AuthedRoutes
import ru.skelantros.easymacher.entities.User

object Types {
  type OrThrowable[A] = Either[Throwable, A]
}
