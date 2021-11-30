package ru.skelantros.easymacher.doobieimpl.user

import doobie.{ConnectionIO, Update0}
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email
import doobie.implicits._
import cats.implicits._

object UserQueries {
  case class Note(user_id: Int, email: String, username: String, activate_token: String, is_activated: Boolean,
                  passw: String, first_name: Option[String], last_name: Option[String], is_admin: Boolean) {
    def toUser: User =
      User(user_id, username, Email.unsafe(email), passw, if(is_admin) Role.Admin else Role.User,
        is_activated, activate_token, first_name, last_name)
  }

  private implicit class UpdNote(upd: Update0) {
    def note: ConnectionIO[Note] =
      upd.withUniqueGeneratedKeys[Note]("user_id", "email", "username", "activate_token", "is_activated", "passw", "first_name", "last_name", "is_admin")
  }

  private val selectAllFr = fr"select user_id, email, username, activate_token, is_activated, passw, first_name, last_name, is_admin from users"

  def selectAll: ConnectionIO[List[Note]] =
    sql"$selectAllFr".query[Note].to[List]

  def selectByAdmin(isAdmin: Boolean): ConnectionIO[List[Note]] =
    sql"$selectAllFr where is_admin = $isAdmin".query[Note].to[List]

  def selectById(id: Int): ConnectionIO[Option[Note]] =
    sql"$selectAllFr where user_id = $id".query[Note].option

  def selectByUsername(username: String): ConnectionIO[Option[Note]] =
    sql"$selectAllFr where lower(username) = ${username.toLowerCase}".query[Note].option

  def selectByEmail(email: String): ConnectionIO[Option[Note]] =
    sql"$selectAllFr where lower(email) = ${email.toLowerCase}".query[Note].option

  def selectByToken(token: String): ConnectionIO[Option[Note]] =
    sql"$selectAllFr where activate_token = $token".query[Note].option

  def update(id: Int,
             email: Option[String], username: Option[String],
             firstName: Option[String], lastName: Option[String]): ConnectionIO[Note] = {
    val fields1 =
      (fr"email", email) :: (fr"username", username) ::
        (fr"first_name", firstName) :: (fr"last_name", lastName) :: Nil
    val frs = fields1.collect {
      case (fr, Some(value)) => fr"$fr = $value"
    }
    (fr"update users set " ++ frs.intercalate(fr",") ++ fr"where user_id = $id")
      .update.note
  }


  def create(username: String, passw: String, email: String, isAdmin: Boolean, token: String): ConnectionIO[Note] =
    sql"insert into users(username, passw, email, is_admin, activate_token, is_activated) values ($username, $passw, $email, $isAdmin, $token, false)"
    .update.note

  def activate(token: String): ConnectionIO[Note] =
    sql"update users set is_activated = true where activate_token = $token"
    .update.note
}
