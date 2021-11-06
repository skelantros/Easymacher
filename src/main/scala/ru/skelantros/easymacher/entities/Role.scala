package ru.skelantros.easymacher.entities

sealed trait Role {
  def asString: String
}
object Role {
  case object User extends Role {
    override def asString: String = "user"
  }
  case object Admin extends Role {
    override def asString: String = "admin"
  }
}