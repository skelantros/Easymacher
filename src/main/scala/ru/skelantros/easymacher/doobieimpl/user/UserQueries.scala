package ru.skelantros.easymacher.doobieimpl.user

import doobie.{ConnectionIO, Update0}
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email
import doobie.implicits._
import cats.implicits._
import ru.skelantros.easymacher.doobieimpl.DoobieLogging

object UserQueries extends DoobieLogging {
  case class Note(user_id: Int, email: String, username: String, activate_token: String, is_activated: Boolean,
                  passw: String, first_name: Option[String], last_name: Option[String], is_admin: Boolean) {
    def toUser: User =
      User(user_id, username, Email.unsafe(email), passw, if(is_admin) Role.Admin else Role.User,
        is_activated, activate_token, first_name, last_name)
  }

  implicit class UpdNote(upd: Update0) {
    def note: ConnectionIO[Note] =
      upd.withUniqueGeneratedKeys[Note]("user_id", "email", "username", "activate_token", "is_activated", "passw", "first_name", "last_name", "is_admin")
  }

  val selectAllFr = fr"select user_id, email, username, activate_token, is_activated, passw, first_name, last_name, is_admin from users"

  def selectAll =
    sql"$selectAllFr".query[Note]

  def selectByAdmin(isAdmin: Boolean) =
    sql"$selectAllFr where is_admin = $isAdmin".query[Note]

  def selectById(id: Int) =
    sql"$selectAllFr where user_id = $id".query[Note]

  def selectByUsername(username: String) =
    sql"$selectAllFr where lower(username) = ${username.toLowerCase}".query[Note]

  def selectByEmail(email: String) =
    sql"$selectAllFr where lower(email) = ${email.toLowerCase}".query[Note]

  def selectByToken(token: String) =
    sql"$selectAllFr where activate_token = $token".query[Note]

  // Выбрасывает исключение, если пользователь ничего не обновляет (все поля - None)
  def update(id: Int,
             email: Option[String], username: Option[String],
             firstName: Option[String], lastName: Option[String]): Update0 = {
    val fields1 =
      (fr"email", email.map(_.toLowerCase)) :: (fr"username", username.map(_.toLowerCase)) ::
        (fr"first_name", firstName) :: (fr"last_name", lastName) :: Nil
    val frs = fields1.collect {
      case (fr, Some(value)) => fr"$fr = $value"
    }
    (fr"update users set " ++ frs.intercalate(fr",") ++ fr"where user_id = $id")
      .update
  }


  def create(username: String, passw: String, email: String, isAdmin: Boolean, token: String): Update0 =
    sql"""insert into users(username, passw, email, is_admin, activate_token, is_activated)
          values (${username.toLowerCase}, $passw, ${email.toLowerCase}, $isAdmin, $token, false)"""
    .update

  def activate(token: String): Update0 =
    sql"update users set is_activated = true where activate_token = $token".update
}
