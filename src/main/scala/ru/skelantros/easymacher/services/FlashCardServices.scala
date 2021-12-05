package ru.skelantros.easymacher.services

import cats.effect.kernel.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import ru.skelantros.easymacher.db.{DbResult, DbUnit}
import ru.skelantros.easymacher.entities.{FlashCards, User}
import FlashCards.{FlashDesc => Desc}
import ru.skelantros.easymacher.utils.StatusMessages
import ru.skelantros.easymacher.db.FlashCardDb._
import org.http4s.implicits._
import cats.implicits._
import io.circe.generic.auto._

class FlashCardServices[F[_] : Concurrent] {
  import FlashCardServices._

  private val dsl = new Http4sDsl[F] {}
  import dsl._

  private def visibleAction(dbDesc: DbResult[Desc], u: User)
                           (ifAccess: Desc => F[Response[F]]): F[Response[F]] =
    dbDesc match {
      case Right(desc) if desc.isVisibleTo(u) =>
        ifAccess(desc)
      case Right(desc) =>
        Forbidden(StatusMessages.noAccessToFlashCards(desc.id))
      case Left(error) =>
        responseWithError[F](error)
    }
  private def editAction(dbDesc: DbResult[Desc], u: User)
                        (ifAccess: Desc => F[Response[F]]): F[Response[F]] =
    dbDesc match {
      case Right(desc) if desc.isEditedBy(u) =>
        ifAccess(desc)
      case Right(desc) =>
        Forbidden(StatusMessages.cannotEditFlashCards(desc.id))
      case Left(error) =>
        responseWithError[F](error)
    }

  def allServices(implicit db: DescSelect[F], db2: Select[F], db3: Update[F]): User => HttpRoutes[F] =
    u => allVisible(u) <+> byIdVisible(u) <+> wordsOfGroup(u) <+> create(u) <+> add(u) <+> update(u)

  def allVisible(u: User)(implicit db: DescSelect[F]) = HttpRoutes.of[F] {
    case GET -> Root / "flash-cards" =>
      processDbDef(db.allDescs)(_.filter(_.isVisibleTo(u)).map(JsonOut(_)))
  }

  def byIdVisible(u: User)(implicit db: DescSelect[F]) = HttpRoutes.of[F] {
    case GET -> Root / "flash-cards" / IntVar(id) =>
      for {
        dbRes <- db.descById(id)
        resp <- visibleAction(dbRes, u)(d => Ok(JsonOut(d)))
      } yield resp
  }

  def wordsOfGroup(u: User)(implicit db: Select[F]) = HttpRoutes.of[F] {
    case GET -> Root / "flash-cards" / IntVar(id) / "words" =>
      for {
        dbRes <- db.flashCardsById(id)
        resp <- dbRes match {
          case Right(group) if group.isVisibleTo(u) =>
            Ok(group.words.map(WordServices.JsonOut.fromWord))
          case Right(_) =>
            Forbidden(StatusMessages.noAccessToFlashCards(id))
          case Left(error) =>
            responseWithError[F](error)
        }
      } yield resp
  }

  def create(u: User)(implicit db: Update[F]) = HttpRoutes.of[F] {
    case req @ POST -> Root / "flash-cards" / "create" =>
      for {
        json <- req.as[JsonCreate]
        JsonCreate(name, isShared) = json
        resp <- processDbDef(db.createFlashCards(u, name, isShared))(identity)
      } yield resp
  }

  private def addGroupsWords(id: Int, words: Option[Seq[Int]], groups: Option[Seq[Int]])(implicit dbUpd: Update[F]): F[Response[F]] = {
    val dbQuery = for {
      wordsRes <- words.map(w => dbUpd.addWordsByIds(id, w)).getOrElse(DbResult.unit.pure[F])
      res <- wordsRes match {
        case Right(()) =>
          groups
            .map(g => g.map(dbUpd.addWordsByGroupId(id, _)).sequence.map(_ => DbResult.unit))
            .getOrElse(DbResult.unit.pure[F])
        case Left(e) => DbUnit.error(e).pure[F]
      }
    } yield res

    processDbDef(dbQuery)(identity)
  }

  // TODO этот запрос к сервису не обваливается, если в флеш карточке есть слова из группы
  // Это правильное поведение, но непонятно, с чем оно связано
  def add(u: User)(implicit dbSel: DescSelect[F], dbUpd: Update[F]) = HttpRoutes.of[F] {
    case req @ POST -> Root / "flash-cards" / IntVar(id) / "add" =>
      for {
        descDb <- dbSel.descById(id)
        json <- req.as[JsonAdd]
        JsonAdd(wordsOpt, groupsOpt) = json

        resp <- editAction(descDb, u)(d => addGroupsWords(d.id, wordsOpt, groupsOpt))
      } yield resp
  }

  def update(u: User)(implicit dbSel: DescSelect[F], dbUpd: Update[F]) = HttpRoutes.of[F] {
    case req @ POST -> Root / "flash-cards" / IntVar(id) / "update" =>
      for {
        descDb <- dbSel.descById(id)
        json <- req.as[JsonUpdate]
        JsonUpdate(isShared, name) = json

        resp <- editAction(descDb, u)(d => processDbDef(dbUpd.update(d.id, name, isShared))(identity))
      } yield resp
  }
}

object FlashCardServices {
  case class JsonCreate(name: String, isShared: Boolean)
  object JsonCreate {
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonCreate] = jsonOf
  }

  case class JsonAdd(words: Option[Seq[Int]], groups: Option[Seq[Int]])
  object JsonAdd {
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonAdd] = jsonOf
  }

  case class JsonUpdate(isShared: Option[Boolean], name: Option[String])
  object JsonUpdate {
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonUpdate] = jsonOf
  }

  case class JsonOut(id: Int, owner: Int, name: String)
  object JsonOut {
    def apply(desc: Desc): JsonOut =
      JsonOut(desc.id, desc.ownerId, desc.name)

    implicit def encoder[F[_]]: EntityEncoder[F, JsonOut] = dropJsonEnc
    implicit def seqEncoder[F[_]]: EntityEncoder[F, Seq[JsonOut]] = dropJsonEnc
  }
}
