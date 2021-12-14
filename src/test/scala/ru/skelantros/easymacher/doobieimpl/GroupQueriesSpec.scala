package ru.skelantros.easymacher.doobieimpl
import group.GroupQueries._

class GroupQueriesSpec extends AnalyzeSpec {
  test("selectGroups") { check(selectGroups) }
  test("selectGroupsByUserId") { check(selectGroupsByUserId(1)) }
  test("selectGroupById") { check(selectGroupById(1)) }
  test("insertGroup") { check(insertGroup(1, "name", true)) }
  test("updateGroup NN") { check(updateGroup(1, None, None)) }
  test("updateGroup SN") { check(updateGroup(1, Some("name"), None)) }
  test("updateGroup NS") { check(updateGroup(1, None, Some(true))) }
  test("updateGroup SS") { check(updateGroup(1, Some("name"), Some(true))) }
  test("selectG2W") { check(selectG2W) }
  test("selectG2WByGroupId") { check(selectG2WByGroupId(1)) }
  test("insertG2W") { check(insertG2W(1, 1)) }
  test("deleteGroup") { check(deleteGroup(1)) }
}
