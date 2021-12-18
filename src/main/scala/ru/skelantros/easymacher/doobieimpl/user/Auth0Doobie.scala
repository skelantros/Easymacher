package ru.skelantros.easymacher.doobieimpl.user

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.{DbResult, UserDb}
import ru.skelantros.easymacher.entities.User
import ru.skelantros.easymacher.doobieimpl._
import Auth0Queries._
import UserQueries.UpdNote

class Auth0Doobie[F[_] : Async](implicit xa: Transactor[F]) extends UserDb.Auth0[F] {
  override def findByAuth0Id(auth0Id: String): F[DbResult[Option[User]]] =
    processSelect(findByAuth0Sub(auth0Id).option)(_.map(_.toUser))

  override def addByAuth0Id(auth0Id: String): F[DbResult[User]] =
    processSelect(addByAuth0Sub(auth0Id, false).note)(_.toUser)

  override def addAuth0User(auth0Id: String, username: String, firstName: Option[String], lastName: Option[String]): F[DbResult[User]] =
    processSelect(addByAuth0Info(auth0Id, username, firstName, lastName).note)(_.toUser)
}
