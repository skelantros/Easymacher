package ru.skelantros.easymacher.entities

sealed trait Word {
  def word: String
  def translate: Option[String]
  def owner: User
}

case class AnyWord(word: String, translate: Option[String], owner: User) extends Word

case class Noun(word: String, translate: Option[String], owner: User,
                gender: Noun.Gender, plural: Option[String]) extends Word
object Noun {
  sealed trait Gender
  object Gender {
    case object M extends Gender
    case object F extends Gender
    case object N extends Gender
  }
}

object Word {
  sealed trait Type
  object Type {
    case object Noun extends Type
    case object Any extends Type
  }

  trait Database[F[_]] {
    def allWords: F[Seq[Word]]
    def wordById(id: Int): F[Option[Word]]
    def wordsByUserId(userId: Int): F[Seq[Word]]
    def wordsByUser(user: User): F[Seq[Word]] = wordsByUserId(user.id)

    def addWord(word: String, translate: Option[String], userId: Int): F[DbResult]
    def addWord(word: String, userId: Int): F[DbResult] = addWord(word, None, userId)
    def addWord(word: String, translate: Option[String], user: User): F[DbResult] = addWord(word, translate, user.id)
    def addWord(word: String, user: User): F[DbResult] = addWord(word, None, user.id)

    def addNoun(word: String, translate: Option[String],
                gender: Noun.Gender, plural: Option[String],
                userId: Int): F[DbResult]
    def addNoun(word: String, translate: Option[String],
                gender: Noun.Gender, plural: Option[String],
                user: User): F[DbResult] = addNoun(word, translate, gender, plural, user.id)
    def makeAny(id: Int): F[DbResult]

    def removeWord(id: Int): F[DbResult]
  }

  trait NounDatabase[F[_]] extends Database[F] {
    def makeNoun(id: Int, gender: Noun.Gender, plural: Option[String]): F[DbResult]
  }
}