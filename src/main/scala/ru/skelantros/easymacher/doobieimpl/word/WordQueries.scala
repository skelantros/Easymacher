package ru.skelantros.easymacher.doobieimpl.word

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.update.Update0
import ru.skelantros.easymacher.doobieimpl.DoobieLogging

object WordQueries extends DoobieLogging {
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

  def insertBase(userId: Int, word: String, translate: Option[String], info: Option[String], hasType: Boolean) =
    sql"""insert into words_base(user_id, word, w_translate, w_info, has_type)
          values ($userId, $word, $translate, $info, $hasType)""".update

  def insertNoun(wordId: Int, plural: Option[String], gender: Gender) =
    sql"""insert into words_nouns(word_id, plural, n_gender)
          values ($wordId, $plural, ${gender.value}::gender)""".update

  implicit class UpdNote(update: Update0) {
    def baseNote: ConnectionIO[BaseNote] =
      update.withUniqueGeneratedKeys[BaseNote]("word_id", "user_id", "word", "w_translate", "w_info")
  }

  def deleteBase(wordId: Int) =
    sql"delete from words_base where word_id = $wordId"
  def deleteNoun(wordId: Int) =
    sql"delete from words_nouns where word_id = $wordId"
}
