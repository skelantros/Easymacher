package ru.skelantros.easymacher.doobieimpl.group

import doobie.implicits._
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.util.log.LogHandler
import doobie.util.update.Update0
import ru.skelantros.easymacher.doobieimpl.DoobieLogging
import ru.skelantros.easymacher.entities.WordGroup.Desc

object GroupQueries extends DoobieLogging {
  case class GroupNote(id: Int, ownerId: Int, name: String, isShared: Boolean) {
    def toDesc: Desc = Desc(id, ownerId, name, isShared)
  }
  case class GroupToWordNote(groupId: Int, wordId: Int)

  implicit class UpdateToDesc(upd: Update0) {
    def groupNote: ConnectionIO[GroupNote] =
      upd.withUniqueGeneratedKeys[GroupNote]("group_id", "user_id", "g_name", "is_shared")
  }

  private val groupSelectFr = fr"select group_id, user_id, g_name, is_shared from word_groups"
  private val groupToWordSelectFr = fr"select group_id, word_id from groups_to_words"

  def selectGroups = groupSelectFr.query[GroupNote]
  def selectGroupById(id: Int) = sql"$groupSelectFr where group_id = $id".query[GroupNote]
  def selectGroupsByUserId(userId: Int) = sql"$groupSelectFr where user_id = $userId".query[GroupNote]
  def insertGroup(userId: Int, name: String, isShared: Boolean) =
    sql"insert into word_groups(user_id, g_name, is_shared) values ($userId, $name, $isShared)".update

  def updateGroup(groupId: Int, name: Option[String], isShared: Option[Boolean]): Update0 = ((name, isShared) match {
    case (None, None) => sql""
    case (Some(name), None) =>
      sql"update word_groups set g_name = $name where group_id = $groupId"
    case (None, Some(isShared)) =>
      sql"update word_groups set is_shared = $isShared where group_id = $groupId"
    case (Some(name), Some(isShared)) =>
      sql"update word_groups set is_shared = $isShared, g_name = $name where group_id = $groupId"
  }).update

  def selectG2W = groupToWordSelectFr.query[GroupToWordNote]
  def selectG2WByGroupId(groupId: Int) =
    sql"$groupToWordSelectFr where group_id = $groupId".query[GroupToWordNote]
  def insertG2W(groupId: Int, wordId: Int) =
    sql"insert into groups_to_words(group_id, word_id) values ($groupId, $wordId)".update

  def deleteAllByWordId(wordId: Int) =
    sql"delete from groups_to_words where word_id = $wordId"

  def deleteGroup(groupId: Int): Update0 =
    sql"delete from word_groups where group_id = $groupId".update

  def deleteAllG2WByGroupId(id: Int) =
    sql"delete from groups_to_words where group_id = $id".update

}
