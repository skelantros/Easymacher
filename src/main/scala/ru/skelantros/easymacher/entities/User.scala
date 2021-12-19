package ru.skelantros.easymacher.entities

import ru.skelantros.easymacher.utils.Email
import ru.skelantros.easymacher.utils.Types.OrThrowable

case class User(id: Int, username: String, email: Email, password: String, role: Role, isActivated: Boolean,
                activateToken: String, firstName: Option[String] = None, lastName: Option[String] = None)