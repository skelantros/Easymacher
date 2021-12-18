package ru.skelantros.easymacher.doobieimpl.user

import java.util.UUID

import doobie.util.query.Query0
import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.update.Update0
import ru.skelantros.easymacher.doobieimpl.DoobieLogging

// TODO Отвратительный код, избавиться от него
object Auth0Queries extends DoobieLogging {
  import UserQueries._

  case class Auth0Note(id: Int, username: String, password: String, email: String, isAdmin: Boolean, token: String, isActivated: Boolean, auth0Sub: String)

  def findByAuth0Sub(auth0Sub: String): Query0[Note] =
    sql"$selectAllFr where auth0_sub = $auth0Sub".query[Note]

  private def uuid: String =
    UUID.randomUUID().toString.filter(_ != '-')

  def addByAuth0Sub(auth0Sub: String, isAdmin: Boolean, password: String = "", isActivated: Boolean = true): Update0 = {
    val curUuid = uuid
    val email = s"$curUuid@fromauth0.ru"

    sql"""insert into users(username, passw, email, is_admin, activate_token, is_activated, auth0_sub)
          values ($auth0Sub, $password, $email, $isAdmin, $curUuid, $isActivated, $auth0Sub)""".update
  }

  def addByAuth0Info(auth0Sub: String, username: String, firstName: Option[String], lastName: Option[String]): Update0 = {
    val curUuid = uuid
    val email = s"$curUuid@fromauth0.ru"

    sql"""insert into users(username, passw, email, is_admin, activate_token, is_activated, auth0_sub, first_name, last_name)
         values ($username, ${""}, $email, ${false}, ${curUuid}, ${true}, $auth0Sub, $firstName, $lastName)
       """.update
  }
}
