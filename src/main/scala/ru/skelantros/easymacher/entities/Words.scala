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