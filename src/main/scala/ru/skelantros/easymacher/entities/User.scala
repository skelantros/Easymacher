package ru.skelantros.easymacher.entities

import ru.skelantros.easymacher.utils.Types.OrThrowable

case class User(id: Int, username: String, email: String, role: Role, isActivated: Boolean,
                firstName: Option[String], lastName: Option[String])