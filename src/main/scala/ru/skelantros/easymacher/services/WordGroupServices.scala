package ru.skelantros.easymacher.services

import cats.implicits._
import cats.effect.kernel.Concurrent
import io.circe.Encoder
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}
import org.http4s.circe._
import ru.skelantros.easymacher.db.{DbResult, DbUnit}
import ru.skelantros.easymacher.db.WordGroupDb._
import ru.skelantros.easymacher.entities.{User, WordGroup}
import ru.skelantros.easymacher.utils.StatusMessages

class WordGroupServices[F[_] : Concurrent] {
  import WordGroupServices._

  private val dsl = new Http4sDsl[F] {}
  import dsl._

  private def accessAction(dbDesc: DbResult[WordGroup.Desc], u: User)
                          (ifAccess: WordGroup.Desc => F[Response[F]]): F[Response[F]] =
    dbDesc match {
      case Right(desc) if desc.isVisibleTo(u) =>
        ifAccess(desc)
      case Right(desc) =>
        Forbidden(StatusMessages.noAccessToGroup(desc.id))
      case Left(error) =>
        responseWithError[F](error)
    }

  def allVisible(implicit db: DescSelect[F]) = (u: User) => HttpRoutes.of[F] {
    case GET -> Root / "word-groups" =>
      processDbDef(db.allDescs)(_.filter(_.isVisibleTo(u)).map(JsonOut(_)))
  }

  def byIdVisible(implicit db: DescSelect[F]) = (u: User) => HttpRoutes.of[F] {
    case GET -> Root / "word-groups" / IntVar(id) =>
      for {
        dbRes <- db.descById(id)
        resp <- accessAction(dbRes, u)(d => Ok(JsonOut(d)))
      } yield resp
  }

  def wordsOfGroup(implicit db: Select[F]) = (u: User) => HttpRoutes.of[F] {
    case GET -> Root / "word-groups" / IntVar(id) / "words" =>
      for {
        dbRes <- db.groupWithWordsById(id)
        resp <- dbRes match {
          case Right(group) if group.isVisibleTo(u) =>
            Ok(group.words.map(WordServices.JsonOut.fromWord))
          case Right(_) =>
            Forbidden(StatusMessages.noPermission)
          case Left(error) =>
            responseWithError[F](error)
        }
      } yield resp
  }

  def create(implicit db: Update[F]) = (u: User) => HttpRoutes.of[F] {
    case req @ POST -> Root / "word-groups" / "create" =>
      for {
        json <- req.as[JsonCreate]
        JsonCreate(name, isShared) = json
        resp <- processDbDef(db.createGroup(u, name, isShared))(identity)
      } yield resp
  }


  def addWords(implicit dbSel: DescSelect[F], dbUpd: Update[F]) = (u: User) => HttpRoutes.of[F] {
    case req @ POST -> Root / "words-groups" / IntVar(id) / "add-words" =>
      for {
        descDb <- dbSel.descById(id)
        json <- req.as[JsonAddWords]
        words = json.words

        resp <- accessAction(descDb, u)(d => processDbDef(dbUpd.addWordsByIds(d.id, words))(identity))
      } yield resp
  }

  def update(implicit dbSel: DescSelect[F], dbUpd: Update[F]) = (u: User) => HttpRoutes.of[F] {
    case req @ POST -> Root / "words-groups" / IntVar(id) / "update" =>
      for {
        descDb <- dbSel.descById(id)
        json <- req.as[JsonUpdate]
        JsonUpdate(isShared, name) = json

        resp <- accessAction(descDb, u)(d => processDbDef(dbUpd.update(d.id, name, isShared))(identity))
      } yield resp
  }
}

object WordGroupServices {
  case class JsonCreate(name: String, isShared: Boolean)
  object JsonCreate {
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonCreate] = jsonOf
  }

  case class JsonAddWords(words: Seq[Int])
  object JsonAddWords {
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonAddWords] = jsonOf
  }

  case class JsonUpdate(isShared: Option[Boolean], name: Option[String])
  object JsonUpdate {
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonUpdate] = jsonOf
  }

  case class JsonOut(id: Int, owner: Int, name: String)
  object JsonOut {
    def apply(desc: WordGroup.Desc): JsonOut =
      JsonOut(desc.id, desc.ownerId, desc.name)

    implicit def encoder[F[_]]: EntityEncoder[F, JsonOut] = dropJsonEnc
    implicit def seqEncoder[F[_]]: EntityEncoder[F, Seq[JsonOut]] = dropJsonEnc
  }
}