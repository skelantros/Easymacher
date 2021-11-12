package ru.skelantros.easymacher.entities

sealed trait Word {
  def id: Int
  def word: String
  def translate: Option[String]
  def owner: User
}

case class AnyWord(id: Int, word: String, translate: Option[String], owner: User) extends Word

case class Noun(id: Int, word: String, translate: Option[String], owner: User,
                gender: Noun.Gender, plural: Option[String]) extends Word
object Noun {
  def apply(id: Int, word: String, translate: Option[String], owner: User,
            gender: Noun.Gender, plural: Option[String]): Noun =
    new Noun(id, word.capitalize, translate, owner, gender, plural.map(_.capitalize))

  sealed trait Gender
  object Gender {
    case object M extends Gender
    case object F extends Gender
    case object N extends Gender
  }
}