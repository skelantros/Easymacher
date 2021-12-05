package ru.skelantros.easymacher.doobieimpl.flashcard

import ru.skelantros.easymacher.entities.FlashCards.{FlashDesc => Desc}
import doobie.implicits._
import doobie.util.update.Update0

object FlashCardQueries {
  case class FCNote(id: Int, ownerId: Int, name: String, isShared: Boolean) {
    def toDesc: Desc = Desc(id, name, ownerId, isShared)
  }
  case class FC2WNote(fcId: Int, wordId: Int)

  private val groupSelectFr = fr"select cards_id, user_id, g_name, is_shared from flash_cards"
  private val groupToWordSelectFr = fr"select cards_id, word_id from flash_cards_to_words"

  def selectCards = groupSelectFr.query[FCNote]
  def selectCardsById(id: Int) = sql"$groupSelectFr where cards_id = $id".query[FCNote]
  def selectCardsByUserId(userId: Int) = sql"$groupSelectFr where user_id = $userId".query[FCNote]
  def insertCards(userId: Int, name: String, isShared: Boolean) =
    sql"insert into flash_cards(user_id, g_name, is_shared) values ($userId, $name, $isShared)".update

  def updateCards(cardsId: Int, name: Option[String], isShared: Option[Boolean]): Update0 = ((name, isShared) match {
    case (None, None) => sql""
    case (Some(name), None) =>
      sql"update flash_cards set g_name = $name where cards_id = $cardsId"
    case (None, Some(isShared)) =>
      sql"update flash_cards set is_shared = $isShared where cards_id = $cardsId"
    case (Some(name), Some(isShared)) =>
      sql"update flash_cards set is_shared = $isShared, g_name = $name where cards_id = $cardsId"
  }).update

  def selectFC2W = groupToWordSelectFr.query[FC2WNote]
  def selectFC2WByCardsId(cardsId: Int) =
    sql"$groupToWordSelectFr where cards_id = $cardsId".query[FC2WNote]
  def insertFC2W(cardsId: Int, wordId: Int) =
    sql"insert into flash_cards_to_words(cards_id, word_id) values ($cardsId, $wordId)".update
}
