package ru.skelantros.easymacher.doobieimpl

import user.Auth0Queries._

class Auth0QueriesSpec extends AnalyzeSpec {
  test("findAuth0") { check(findByAuth0Sub("")) }
  test("addAuth0") { check(addByAuth0Sub("", false))}
  test("addByAuth0Info") { check(addByAuth0Info("1", "", Some(""), None))}
}