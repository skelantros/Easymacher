package ru.skelantros.easymacher.db

import cats.Monad
import ru.skelantros.easymacher.entities.WordGroup
import WordGroupDb._
import cats.implicits._
import ru.skelantros.easymacher.entities.WordGroup.Desc
import ru.skelantros.easymacher.utils.StatusMessages

import scala.collection.mutable.ArrayBuffer

class WordGroupMock[F[_] : Monad](init: Seq[WordGroup],
                                  val userDb: UserDb.Select[F],
                                  val wordDb: WordDb.Select[F]) extends DescSelect[F] with Select[F] with Update[F] {
  private val groups = ArrayBuffer.from(init)

  private def nextId: Int = groups.maxBy(_.id).id + 1

  override def allDescs: F[DbResult[Seq[WordGroup.Desc]]] =
    DbResult.of(groups.toSeq.map(Desc(_))).pure[F]

  override def descById(id: Int): F[DbResult[WordGroup.Desc]] =
    groupWithWordsById(id).map(_.map(Desc(_)))

  override def descsByOwner(ownerId: Int): F[DbResult[Seq[Desc]]] =
    DbResult.of(groups.toSeq.collect {
      case g if g.ownerId == ownerId => Desc(g)
    }).pure[F]

  override def allGroups: F[DbResult[Seq[WordGroup]]] = DbResult.of(groups.toSeq).pure[F]

  override def groupWithWordsById(id: Int): F[DbResult[WordGroup]] =
    groups.find(_.id == id) match {
      case Some(g) => DbResult.of(g).pure[F]
      case None => DbResult.mistake[WordGroup](StatusMessages.noGroupById(id)).pure[F]
    }

  override def createGroup(userId: Int, name: String, isShared: Boolean): F[DbResult[Desc]] =
    for {
      userRes <- userDb.userById(userId)
    } yield userRes.map { u =>
      val group = WordGroup(nextId, u, name, isShared, Seq())
      groups += group
      WordGroup.Desc(group)
    }

  override def addWordsByIds(id: Int, wordsIds: Seq[Int]): F[DbUnit] =
    for {
      groupRes <- groupWithWordsById(id)
      dbSeq <- wordsIds.map(wordDb.wordById).sequence
      wordsRes = dbSeq.sequence
      res = (groupRes, wordsRes) match {
        case (Right(g), Right(ws)) =>
          val idxOf = groups.indexOf(g)
          val oldWords = g.words
          groups(idxOf) = g.copy(words = oldWords ++ ws)
          DbResult.unit
        case (Left(e), _) => Left(e)
        case (_, Left(e)) => Left(e)
      }
    } yield res

  override def update(id: Int, name: Option[String], isShared: Option[Boolean]): F[DbResult[Desc]] =
    for {
      groupRes <- groupWithWordsById(id)
      res = groupRes.map { g =>
        val idxOf = groups.indexOf(g)
        val newName = name.getOrElse(g.name)
        val newShared = isShared.getOrElse(g.isShared)
        groups(idxOf) = g.copy(isShared = newShared, name = newName)
        WordGroup.Desc(groups(idxOf))
      }
    } yield res

  override def remove(id: Int): F[DbUnit] = {
    val idx = groups.indexWhere(_.id == id)
    if(idx != -1) groups.remove(idx)
    DbResult.unit.pure[F]
  }
}
