package ru.skelantros.easymacher.doobieimpl

import flashcard.FlashCardQueries._

class FlashCardQueriesSpec extends AnalyzeSpec {
  test("selectCards") { check(selectCards) }
  test("selectCardsByUserId") { check(selectCardsByUserId(1)) }
  test("selectCardsById") { check(selectCardsById(1)) }
  test("insertCards") { check(insertCards(1, "name", true)) }
  test("updateCards NN") { check(updateCards(1, None, None)) }
  test("updateCards SN") { check(updateCards(1, Some("name"), None)) }
  test("updateCards NS") { check(updateCards(1, None, Some(true))) }
  test("updateCards SS") { check(updateCards(1, Some("name"), Some(true))) }
  test("selectFC2W") { check(selectFC2W) }
  test("selectFC2WByCardsId") { check(selectFC2WByCardsId(1)) }
  test("insertFC2W") { check(insertFC2W(1, 1)) }
}
