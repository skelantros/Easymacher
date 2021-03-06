package ru.skelantros.easymacher.utils

object StatusMessages {
  val noPermission: String = "Permission denied"
  val noGenderNoun: String = "Can't add noun without gender."

  def noUserById(id: Int): String = s"User with id $id does not exist."
  def noUserByUsername(username: String): String = s"User with username '$username' does not exist."
  def noUserByToken(token: String): String = s"Invalid token '$token'."
  def cannotEditUser(id: Int) = s"You cannot edit/remove user with id $id."

  def userByUsernameExists(username: String): String = s"User with username '$username' already exists."
  def userAlreadyActivated(token: String): String = s"User with token '$token' has been already activated."

  def noWordById(id: Int): String = s"Word with id $id does not exist."
  def cannotRemoveWord(id: Int): String = s"You cannot remove word with id $id."

  def noAccessToGroup(id: Int): String = s"You do not have access to word group with id $id."
  def cannotEditGroup(id: Int): String = s"You cannot edit the word group with id $id."
  def noGroupById(id: Int): String = s"Word group with id $id does not exist."
  def wordAleadyInGroup(wordId: Int, groupId: Int): String =
    s"Word with id $wordId already exists in the group with id $groupId."

  val invalidPassword: String = "Invalid password."
}
