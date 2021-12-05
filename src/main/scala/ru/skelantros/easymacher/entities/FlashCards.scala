package ru.skelantros.easymacher.entities

case class FlashCards(id: Int, name: String, owner: User, isShared: Boolean, words: Seq[Word]) extends WordGroupLike {
  override def ownerId: Int = owner.id
}

object FlashCards {
  case class FlashDesc(id: Int, name: String, ownerId: Int, isShared: Boolean) extends WordGroupLike
  object FlashDesc {
    def apply(fc: FlashCards): FlashDesc = FlashDesc(fc.id, fc.name, fc.owner.id, fc.isShared)
  }
}