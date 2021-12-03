package ru.skelantros.easymacher.entities

case class WordGroup(id: Int, owner: User, name: String, isShared: Boolean, words: Seq[Word]) {
  def isVisibleTo(userId: Int, userRole: Role): Boolean = {
    if(isShared) true
    else if(owner.id == userId) true
    else if(userRole == Role.Admin) true
    else false
  }
  def isVisibleTo(user: User): Boolean = isVisibleTo(user.id, user.role)
}

object WordGroup {
  case class Desc(id: Int, ownerId: Int, name: String, isShared: Boolean) {
    def isVisibleTo(userId: Int, userRole: Role): Boolean = {
      if(isShared) true
      else if(ownerId == userId) true
      else if(userRole == Role.Admin) true
      else false
    }
    def isVisibleTo(user: User): Boolean = isVisibleTo(user.id, user.role)
  }
}