package ru.skelantros.easymacher.doobieimpl.user

import doobie.{ConnectionIO, Update0}
import ru.skelantros.easymacher.entities.{Role, User}
import doobie.implicits._
import cats.implicits._
import doobie.util.query.Query0
import ru.skelantros.easymacher.doobieimpl.DoobieLogging

object UserQueries extends DoobieLogging {
  case class Note(user_id: Int, auth0Sub: String, username: String, firstName: Option[String], lastName: Option[String], isAdmin: Boolean) {
    def toUser: User =
      User(user_id, username, if(isAdmin) Role.Admin else Role.User, firstName, lastName)
  }

  implicit class UpdNote(upd: Update0) {
    def note: ConnectionIO[Note] =
      upd.withUniqueGeneratedKeys[Note]("user_id", "auth0_sub", "username", "first_name", "last_name", "is_admin")
  }

  val selectAllFr = fr"select user_id, auth0_sub, username, first_name, last_name, is_admin from users"

  def selectAll =
    sql"$selectAllFr".query[Note]

  def selectByAdmin(isAdmin: Boolean) =
    sql"$selectAllFr where is_admin = $isAdmin".query[Note]

  def selectById(id: Int) =
    sql"$selectAllFr where user_id = $id".query[Note]

  def findByAuth0Sub(auth0Sub: String): Query0[Note] =
    sql"$selectAllFr where auth0_sub = $auth0Sub".query[Note]

  // Выбрасывает исключение, если пользователь ничего не обновляет (все поля - None)
  def update(id: Int,
             username: Option[String], firstName: Option[String], lastName: Option[String]): Update0 = {
    val fields1 =
      (fr"username", username.map(_.toLowerCase)) ::
        (fr"first_name", firstName) :: (fr"last_name", lastName) :: Nil
    val frs = fields1.collect {
      case (fr, Some(value)) => fr"$fr = $value"
    }
    (fr"update users set " ++ frs.intercalate(fr",") ++ fr"where user_id = $id")
      .update
  }

  def create(auth0Sub: String, username: String, firstName: Option[String], lastName: Option[String], isAdmin: Boolean): Update0 = {

    sql"""insert into users(username, is_admin, auth0_sub, first_name, last_name)
         values ($username, $isAdmin, $auth0Sub, $firstName, $lastName)
       """.update
  }

  def delete(id: Int): Update0 =
    sql"delete from users where user_id = $id".update
}
