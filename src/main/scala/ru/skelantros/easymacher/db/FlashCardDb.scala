package ru.skelantros.easymacher.db

import ru.skelantros.easymacher.entities.{FlashCards, User, Word, WordGroup}
import FlashCards.{FlashDesc => Desc}


object FlashCardDb {
  trait SelectDesc[F[_]] {
    def allDescs: F[DbResult[Seq[Desc]]]
    def descById: F[DbResult[Desc]]
  }

  trait Select[F[_]] {
    def allFlashCards: F[DbResult[Seq[FlashCards]]]
    def flashCardsById: F[DbResult[FlashCards]]
  }

  trait Update[F[_]] {
    def createFlashCards(userId: Int, name: String, isShared: Boolean): F[DbUnit]
    def createFlashCards(user: User, name: String, isShared: Boolean): F[DbUnit] = createFlashCards(user.id, name, isShared)
    def addWordsByIds(id: Int, wordsSeq: Seq[Int]): F[DbUnit]
    def addWords(id: Int, wordsSeq: Seq[Word]): F[DbUnit] = addWordsByIds(id, wordsSeq.map(_.id))
    def addWordsByGroupId(id: Int, groupId: Int): F[DbUnit]
    def addWordsByGroup(id: Int, group: Desc): F[DbUnit] = addWordsByGroupId(id, group.id)
    def update(id: Int, name: Option[String], isShared: Option[Boolean]): F[DbUnit]
  }
}
