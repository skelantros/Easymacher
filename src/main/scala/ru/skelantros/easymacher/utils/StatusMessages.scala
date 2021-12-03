package ru.skelantros.easymacher.utils

object StatusMessages {
  val noPermission: String = "Permission denied"
  val noGenderNoun: String = "Can't add noun without gender."

  def noUserById(id: Int): String = s"User with id $id does not exist."
  def noUserByUsername(username: String): String = s"User with username '$username' does not exist."
  def noUserByEmail(email: Email): String = s"User with email '${email.asString}' does not exist."
  def noUserByToken(token: String): String = s"Invalid token '$token'."

  def userByUsernameExists(username: String): String = s"User with username '$username' already exists."
  def userByEmailExists(email: Email): String = s"User with email '${email.asString}' already exists."
  def userAlreadyActivated(token: String): String = s"User with token '$token' has been already activated."

  def noAccessToGroup(id: Int): String = s"You do not have access to word group with id $id."

  val invalidPassword: String = "Invalid password."
}
