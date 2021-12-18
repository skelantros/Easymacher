package ru.skelantros.easymacher.db

import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email

object UserDb {
  trait Select[F[_]] {
    def allUsers: F[DbResult[Seq[User]]]
    def usersByRole(role: Role): F[DbResult[Seq[User]]]
    def userById(id: Int): F[DbResult[User]]
    def userByUsername(username: String): F[DbResult[User]]
    def userByEmail(email: Email): F[DbResult[User]]
  }

  trait SelectOffset[F[_]] {
    def allUsers(from: Int, count: Int): F[DbResult[Seq[User]]]
    def usersByRole(role: Role)(from: Int, count: Int): F[DbResult[Seq[User]]]
  }

  trait Update[F[_]] {
    def updatePassword(id: Int, oldPassword: String, newPassword: String): F[DbResult[User]]
    def updateInfo(id: Int, firstName: Option[String], lastName: Option[String],
                   username: Option[String], email: Option[Email]): F[DbResult[User]]

    def updateFirstName(id: Int, firstName: String): F[DbResult[User]] =
      updateInfo(id, Some(firstName), None, None, None)
    def updateLastName(id: Int, lastName: String): F[DbResult[User]] =
      updateInfo(id, None, Some(lastName), None, None)
    def updateUsername(id: Int, username: String): F[DbResult[User]] =
      updateInfo(id, None, None, Some(username), None)
    def updateEmail(id: Int, email: Email): F[DbResult[User]] =
      updateInfo(id, None, None, None, Some(email))
  }

  trait Remove[F[_]] {
    def removeUser(id: Int): F[DbUnit]
  }

  trait Register[F[_]] {
    def createUser(username: String, password: String, email: Email, role: Role): F[DbUnit]
    def activateUser(uuid: String): F[DbUnit]
  }

  trait Auth0[F[_]] {
    def findByAuth0Id(auth0Id: String): F[DbResult[Option[User]]]
    def addByAuth0Id(auth0Id: String): F[DbResult[User]]
    def addAuth0User(auth0Id: String, username: String, firstName: Option[String], lastName: Option[String]): F[DbResult[User]]
  }
}