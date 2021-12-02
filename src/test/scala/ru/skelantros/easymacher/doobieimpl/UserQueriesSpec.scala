package ru.skelantros.easymacher.doobieimpl

import cats.effect.IO
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers
import ru.skelantros.easymacher.utils.TransactorImpl
import user.UserQueries._


class UserQueriesSpec extends AnalyzeSpec {
  test("selectAll") { check(selectAll) }
  test("selectByAdmin") { check(selectByAdmin(true)) }
  test("selectById") { check(selectById(1)) }
  test("selectByUsername") { check(selectByUsername("someone")) }
  test("selectByEmail") { check(selectByEmail("email")) }
  test("selectByToken") { check(selectByToken("token")) }
  test("update 0") { check(update(1, Some("email"), Some("username"), Some("firstName"), Some("lastName"))) }
  // test("update 1") { check(update(1, None, None, None, None)) }
  test("update 2") { check(update(1, Some("email"), None, None, None)) }
  test("update 3") { check(update(1, None, Some("username"), None, None)) }
  test("update 4") { check(update(1, None, None, Some("firstName"), None)) }
  test("update 5") { check(update(1, None, None, None, Some("lastName"))) }
  test("create") { check(create("username", "password", "email", true, "token")) }
  test("activate") { check(activate("token")) }
}
