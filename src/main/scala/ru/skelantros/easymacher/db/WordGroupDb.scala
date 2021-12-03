package ru.skelantros.easymacher.db

import ru.skelantros.easymacher.entities.{User, Word, WordGroup}
import WordGroup.Desc

object WordGroupDb {

  trait DescSelect[F[_]] {
    def allDescs: F[DbResult[Seq[Desc]]]
    def descById(id: Int): F[DbResult[Desc]]
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
