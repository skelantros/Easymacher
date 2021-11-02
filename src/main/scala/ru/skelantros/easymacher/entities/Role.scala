package ru.skelantros.easymacher.entities

sealed trait Role
object Role {
  case object User extends Role
  case object Admin extends Role
}