package ru.skelantros.easymacher.entities

import ru.skelantros.easymacher.utils.Types.OrThrowable

case class User(id: Int, username: String, email: String, role: Role, isActivated: Boolean,
                firstName: Option[String], lastName: Option[String])

object User {
  trait Database[F[_]] {
    import Database._

    def allUsers: F[OrThrowable[Seq[User]]]
    def usersByRole(role: Role): F[OrThrowable[Seq[User]]]
    def userById(id: Int): F[OrThrowable[Option[User]]]
    def userByUsername(username: String): F[OrThrowable[Option[User]]]

    def updatePassword(id: Int, oldPassword: String, newPassword: String): F[UpPassResult]
    def updateInfo(id: Int, firstName: Option[String] = None, lastName: Option[String] = None): F[DbResult]
    def updateRole(id: Int, role: Role): F[DbResult]

    def createUser(id: Int, username: String, password: String, role: Role): F[CreateResult]
    def activateUser(uuid: String): F[DbResult]

    def deleteUser(id: Int): F[DbResult]
  }

  object Database {
    sealed trait UpPassResult extends Product with Serializable
    object UpPassResult {
      case object Success extends UpPassResult
      case object WrongPassword extends UpPassResult
      case class Thr(t: Throwable) extends UpPassResult
    }

    sealed trait CreateResult extends Product with Serializable
    object CreateResult {
      case object UsernameExists extends CreateResult
      case object EmailExists extends CreateResult
      case object Success extends CreateResult
      case class Thr(t: Throwable) extends CreateResult
    }
  }
}