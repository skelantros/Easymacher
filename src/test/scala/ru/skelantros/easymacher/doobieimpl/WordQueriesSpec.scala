package ru.skelantros.easymacher.doobieimpl

import word.WordQueries._
import word.Gender

class WordQueriesSpec extends AnalyzeSpec {
  test("selectAllBase") { check(selectAllBase) }
  test("selectByIdBase") { check(selectByIdBase(1)) }
  test("selectAllByUserIdBase") { check(selectAllByUserIdBase(1)) }
  test("selectAllNouns") { check(selectAllNouns) }
  test("selectByIdNouns") { check(selectByIdNouns(1)) }
  test("selectAllByUserIdNouns") { check(selectAllByUserIdNouns(1)) }
  test("insertBase") { check(insertBase(1, "Hello", None, None)) }
  test("insertNoun M") { check(insertNoun(1, None, Gender.M)) }
  test("insertNoun F") { check(insertNoun(1, None, Gender.F)) }
  test("insertNoun N") { check(insertNoun(1, None, Gender.N)) }
}
