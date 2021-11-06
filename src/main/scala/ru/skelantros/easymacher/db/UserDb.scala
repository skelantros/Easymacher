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
    def updatePassword(id: Int, oldPassword: String, newPassword: String): F[DbUnit]
    def updateInfo(id: Int, firstName: Option[String], lastName: Option[String],
                   username: Option[String], email: Option[String]): F[DbUnit]

    def updateFirstName(id: Int, firstName: String): F[DbUnit]
    def updateLastName(id: Int, lastName: String): F[DbUnit]
    def updateUsername(id: Int, username: String): F[DbUnit]
    def updateEmail(id: Int, email: String): F[DbUnit]
  }

  trait AdminUpdate[F[_]] {
    def updateRole(id: Int, role: Role): F[DbUnit]
    def deleteUser(id: Int): F[DbUnit]
  }

  trait Register[F[_]] {
    def createUser(username: String, password: String, email: String, role: Role): F[DbUnit]
    def activateUser(uuid: String): F[DbUnit]
  }
}