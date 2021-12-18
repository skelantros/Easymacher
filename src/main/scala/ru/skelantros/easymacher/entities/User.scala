package ru.skelantros.easymacher.entities

import ru.skelantros.easymacher.utils.Email
import ru.skelantros.easymacher.utils.Types.OrThrowable

// TODO провести рефакторинг: выделить общий трейт User, от него унаследовать реализации для auth0 и cryptokey
case class User(id: Int, username: String, role: Role, firstName: Option[String] = None, lastName: Option[String] = None, auth0Id: String = "")