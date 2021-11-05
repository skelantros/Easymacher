package ru.skelantros.easymacher.db

import ru.skelantros.easymacher.entities.{Noun, User, Word}

object WordDb {
  trait Select[F[_]] {
    def allWords: F[DbResult[Seq[Word]]]
    def wordById(id: Int): F[DbResult[Word]]
    def wordsByUserId(userId: Int): F[DbResult[Seq[Word]]]
    def wordsByUser(user: User): F[DbResult[Seq[Word]]] = wordsByUserId(user.id)
  }

  trait AddAny[F[_]] {
    def addWord(word: String, translate: Option[String], userId: Int): F[DbUnit]
    def addWord(word: String, userId: Int): F[DbUnit] = addWord(word, None, userId)
    def addWord(word: String, translate: Option[String], user: User): F[DbUnit] = addWord(word, translate, user.id)
    def addWord(word: String, user: User): F[DbUnit] = addWord(word, None, user.id)
  }

  trait AddNoun[F[_]] {
    def addNoun(word: String, translate: Option[String],
                gender: Noun.Gender, plural: Option[String],
                userId: Int): F[DbUnit]
    def addNoun(word: String, translate: Option[String],
                gender: Noun.Gender, plural: Option[String],
                user: User): F[DbUnit] = addNoun(word, translate, gender, plural, user.id)
  }
}
