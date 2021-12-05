package ru.skelantros.easymacher.db

import ru.skelantros.easymacher.entities.{FlashCards, User, Word, WordGroup}
import FlashCards.{FlashDesc => Desc}

/*
  TODO
  Методы, отправляющие в "БД" идентификаторы (см. трейт Update), нежелательны.
  Это связано с тем, что они нарушают систему типов внутри приложения: "БД" не может быть уверена в том,
  что слово с заданным ИД существует, и поэтому необходима проверка на уровне самой "БД".
  В то же время, если в "БД" направляется кейс-объект, система уверена в том, что соответствующее слово существует,
  т.к. кейс-объекты созданы другими "БД" на основе существующих данных.
  Эта уверенность позволяет сделать "БД"-объекты независимыми друг от друга, т.к. нет необходимости делать проверки
  в областях других "БД" (в данном случае для проверки существования слова нужно обратиться к сущности WordDb.Select)
 */
object FlashCardDb {
  trait DescSelect[F[_]] {
    def allDescs: F[DbResult[Seq[Desc]]]
    def descById(id: Int): F[DbResult[Desc]]
  }

  trait Select[F[_]] {
    def allFlashCards: F[DbResult[Seq[FlashCards]]]
    def flashCardsById(id: Int): F[DbResult[FlashCards]]
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
