package ru.skelantros.easymacher.entities

trait WordGroupLike {
  def id: Int
  def ownerId: Int
  def name: String
  def isShared: Boolean

  def isVisibleTo(userId: Int, userRole: Role): Boolean =
    if(isShared) true
    else if(isEditedBy(userId, userRole)) true
    else false
  def isVisibleTo(user: User): Boolean = isVisibleTo(user.id, user.role)

  def isEditedBy(userId: Int, userRole: Role): Boolean =
    if(ownerId == userId) true
    else if(userRole == Role.Admin) true
    else false
  def isEditedBy(user: User): Boolean = isEditedBy(user.id, user.role)
}

case class WordGroup(id: Int, owner: User, name: String, isShared: Boolean, words: Seq[Word]) extends WordGroupLike {
  override def ownerId: Int = owner.id
}

object WordGroup {
  case class Desc(id: Int, ownerId: Int, name: String, isShared: Boolean) extends WordGroupLike
  object Desc {
    def apply(group: WordGroup): Desc =
      Desc(group.id, group.owner.id, group.name, group.isShared)
  }
}