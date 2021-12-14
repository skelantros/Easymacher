package ru.skelantros.easymacher.db

import ru.skelantros.easymacher.entities.{User, Word, WordGroup}
import WordGroup.Desc

/*
  TODO
  Методы, отправляющие в "БД" идентификаторы (метод addWordsByIds в данном случае) нежелательны.
  Это связано с тем, что они нарушают систему типов внутри приложения: "БД" не может быть уверена в том,
  что слово с заданным ИД существует, и поэтому необходима проверка на уровне самой "БД".
  В то же время, если в "БД" направляется кейс-объект, система уверена в том, что соответствующее слово существует,
  т.к. кейс-объекты созданы другими "БД" на основе существующих данных.
  Эта уверенность позволяет сделать "БД"-объекты независимыми друг от друга, т.к. нет необходимости делать проверки
  в областях других "БД" (в данном случае для проверки существования слова нужно обратиться к сущности WordDb.Select).
 */
object WordGroupDb {

  trait DescSelect[F[_]] {
    def allDescs: F[DbResult[Seq[Desc]]]
    def descById(id: Int): F[DbResult[Desc]]
    def descsByOwner(ownerId: Int): F[DbResult[Seq[Desc]]]
  }

  trait Select[F[_]] {
    def allGroups: F[DbResult[Seq[WordGroup]]]
    def groupWithWordsById(id: Int): F[DbResult[WordGroup]]
  }

  trait Update[F[_]] {
    def createGroup(userId: Int, name: String, isShared: Boolean): F[DbUnit]
    def createGroup(user: User, name: String, isShared: Boolean): F[DbUnit] = createGroup(user.id, name, isShared)
    def addWordsByIds(id: Int, wordsIds: Seq[Int]): F[DbUnit]
    def addWords(id: Int, words: Seq[Word]): F[DbUnit] = addWordsByIds(id, words.map(_.id))
    def update(id: Int, name: Option[String], isShared: Option[Boolean]): F[DbUnit]
  }
}
