package ru.skelantros.easymacher.doobieimpl.user

import java.util.UUID

import cats.Monad
import cats.implicits._
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.{DbResult, DbUnit}
import ru.skelantros.easymacher.db.UserDb._
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email
import ru.skelantros.easymacher.doobieimpl._
import ru.skelantros.easymacher.utils.StatusMessages._
import UserQueries._
import cats.effect.kernel.Async

class UserDoobie[F[_] : Async](implicit val xa: Transactor[F])
  extends Select[F] with Update[F] with Remove[F] with Auth0[F] {
  private val note = (n: Note) => n.toUser
  private def seq[W[_] : Monad]: W[Note] => W[User] = (n: W[Note]) => n.map(note)

  override def allUsers: F[DbResult[Seq[User]]] = processSelect(selectAll.to[List])(seq)
  override def usersByRole(role: Role): F[DbResult[Seq[User]]] = processSelect(selectByAdmin(role == Role.Admin).to[List])(seq)

  override def userById(id: Int): F[DbResult[User]] =
    processOptSelect(selectById(id).option)(_.toUser, noUserById(id))

  override def updateInfo(id: Int, firstName: Option[String], lastName: Option[String], username: Option[String], email: Option[Email]): F[DbResult[User]] =
    if(firstName.isEmpty && lastName.isEmpty && username.isEmpty && email.isEmpty)
      userById(id)
    else processSelect(update(id, email.map(_.asString), username, firstName, lastName).note)(_.toUser)

  override def removeUser(id: Int): F[DbUnit] =
    processUpdate(delete(id))

  override def findByAuth0Id(auth0Id: String): F[DbResult[Option[User]]] =
    processSelect(findByAuth0Sub(auth0Id).option)(_.map(_.toUser))

  override def addAuth0User(auth0Id: String, username: String, firstName: Option[String], lastName: Option[String], isAdmin: Boolean): F[DbResult[User]] =
    processSelect(create(auth0Id, username, firstName, lastName, isAdmin).note)(_.toUser)
}