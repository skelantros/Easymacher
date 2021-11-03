package ru.skelantros.easymacher.db

import ru.skelantros.easymacher.entities.{Role, User}

object UserDb {
  trait Select[F[_]] {
    def allUsers: F[DbResult[Seq[User]]]
    def usersByRole(role: Role): F[DbResult[Seq[User]]]
    def userById(id: Int): F[DbResult[Option[User]]]
    def userByUsername(username: String): F[DbResult[Option[User]]]
  }

  trait Update[F[_]] {
    def updatePassword(id: Int, oldPassword: String, newPassword: String): F[DbUnit]
    def updateInfo(id: Int, firstName: Option[String] = None, lastName: Option[String] = None): F[DbUnit]
    def updateRole(id: Int, role: Role): F[DbUnit]
    def deleteUser(id: Int): F[DbUnit]
  }

  trait Register[F[_]] {
    def createUser(username: String, password: String, email: String, role: Role): F[DbUnit]
    def activateUser(uuid: String): F[DbUnit]
  }
}