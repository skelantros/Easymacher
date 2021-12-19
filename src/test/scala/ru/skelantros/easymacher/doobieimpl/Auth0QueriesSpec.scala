package ru.skelantros.easymacher.doobieimpl

class Auth0QueriesSpec extends AnalyzeSpec {
  test("findAuth0") { check(findByAuth0Sub("")) }
  test("addAuth0") { check(addByAuth0Sub("", false))}
}