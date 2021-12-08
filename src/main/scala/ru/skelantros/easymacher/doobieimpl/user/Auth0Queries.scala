package ru.skelantros.easymacher.doobieimpl.user

import doobie.util.query.Query0
import doobie.implicits._
import doobie.util.update.Update0

object Auth0Queries {
  import UserQueries._
  def findByAuth0Sub(auth0Sub: String): Query0[Note] =
    sql"$selectAllFr where auth0_sub = $auth0Sub".query[Note]

  def addByAuth0Sub(auth0Sub: String, isAdmin: Boolean): Update0 =
    sql"""insert into users(username, passw, email, is_admin, activate_token, is_activated, auth0_sub)
          values ($auth0Sub, '', 'auth0@easymacher.ru', $isAdmin, '', true, $auth0Sub)""".update

}
