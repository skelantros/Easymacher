package ru.skelantros.easymacher

import org.http4s.AuthedRoutes
import ru.skelantros.easymacher.entities.User

package object auth {
  type UserRoutes[F[_]] = AuthedRoutes[User, F]
}
