package ru.skelantros.easymacher.doobieimpl.word

import doobie.implicits._

object WordQueries {
  case class BaseNote(id: Int, userId: Int, word: String, translate: Option[String], info: Option[String])
  private val allBaseFr =
    fr"""select word_id, user_id, word, w_translate, w_info
        from words_base
        where has_type = false
      """

  def selectAllBase = allBaseFr.query[BaseNote]
  def selectByIdBase(id: Int) = sql"$allBaseFr and word_id = $id".query[BaseNote]
  def selectAllByUserIdBase(userId: Int) = sql"$allBaseFr and user_id = $userId".query[BaseNote]

  case class NounNote(id: Int, userId: Int, word: String, translate: Option[String], info: Option[String],
                      plural: Option[String], nGender: String) {
    val gender: Gender = Gender(nGender)
  }
  private val allNounFr =
    fr"""
        select words_base.word_id, user_id, word, w_translate, w_info, plural, n_gender
        from words_base join words_nouns
        on words_base.word_id = words_nouns.word_id
      """

  def selectAllNouns = allNounFr.query[NounNote]
  def selectByIdNouns(id: Int) = sql"$allNounFr where words_base.word_id = $id".query[NounNote]
  def selectAllByUserIdNouns(userId: Int) = sql"$allNounFr where user_id = $userId".query[NounNote]

  def insertBase(userId: Int, word: String, translate: Option[String], info: Option[String]) =
    sql"insert into words_base(user_id, word, w_translate, w_info) values ($userId, $word, $translate, $info)".update
  def insertNoun(wordId: Int, plural: Option[String], gender: Gender) =
    sql"insert into words_nouns(word_id, plural, n_gender) values ($wordId, $plural, ${gender.value})".update
}
