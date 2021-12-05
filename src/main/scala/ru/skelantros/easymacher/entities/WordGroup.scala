package ru.skelantros.easymacher.entities

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