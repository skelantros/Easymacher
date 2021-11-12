package ru.skelantros.easymacher.services

import cats.Monad
import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.auto._
import cats.implicits._
import ru.skelantros.easymacher.db.DbResult
import ru.skelantros.easymacher.db.WordDb._
import ru.skelantros.easymacher.entities.{AnyWord, Noun, User, Word}
import ru.skelantros.easymacher.utils.StatusMessages._

//noinspection TypeAnnotation
class WordServices[F[_] : Concurrent] {
  private val dsl = new Http4sDsl[F] {}
  import dsl._
  import WordServices._

  def all(implicit db: Select[F]) = HttpRoutes.of[F] {
    case GET -> Root / "all-words" =>
      processDbDef(db.allWords)(_ map JsonOut.fromWord)
  }
  def byId(implicit db: Select[F]) = HttpRoutes.of[F] {
    case GET -> Root / "all-words" :? IdParam(id) =>
      processDbDef(db.wordById(id))(JsonOut.fromWord)
  }
  def ofUser(user: User)(implicit db: Select[F]) = HttpRoutes.of[F] {
    case GET -> Root / "words" =>
      processDbDef(db.wordsByUser(user))(_ map JsonOut.fromWord)
  }
  def ofUserById(user: User)(implicit db: Select[F]) = HttpRoutes.of[F] {
    case GET -> Root / "words" :? IdParam(id) =>
      processDbDef(
        db.wordById(id).map(_.flatMap { w =>
          if(w.owner == user) DbResult.of(w) else DbResult.mistake(noPermission)
        })
      )(JsonOut.fromWord)
  }
  def addWord(user: User)(implicit db: AddAny[F] with AddNoun[F]) = HttpRoutes.of[F] {
    case req @ POST -> Root / "add-word" =>
      val inp = req.as[JsonIn]
      inp.flatMap { jsonInp =>
        val JsonIn(_, typ, translate, _, plural) = jsonInp
        val word = jsonInp.actualWord
        lazy val dbReq = jsonInp.genderOpt match {
          case Some(g) => db.addNoun(word, translate, g, plural, user)
          case None if !typ.map(_.toLowerCase).contains("noun") => db.addWord(word, translate, user)
          case _ => DbResult.mistake[Unit](noGenderNoun).pure[F]
        }
        processDbDef(dbReq)(identity)
      }
  }
}

object WordServices {
  case class JsonOut(id: Int, word: String, `type`: String, translate: Option[String], owner: Int,
                     gender: Option[String] = None, plural: Option[String] = None // Noun specific fields
                    )
  object JsonOut {
    implicit def encoder[F[_]]: EntityEncoder[F, JsonOut] = dropJsonEnc
    implicit def seqEncoder[F[_]]: EntityEncoder[F, Seq[JsonOut]] = dropJsonEnc

    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonOut] = jsonOf

    def fromWord(word: Word): JsonOut = word match {
      case a: AnyWord => JsonOut(word.id, a.word, "unknown", a.translate, a.owner.id)
      case n: Noun => JsonOut(word.id, n.word, "noun", n.translate, n.owner.id, Some(n.gender.toString.toLowerCase), n.plural)
    }
  }

  case class JsonIn(word: String, `type`: Option[String], translate: Option[String],
                    gender: Option[String], plural: Option[String] // Noun specific fields
                   ) {
    import JsonIn._
    def isNoun: Boolean =
      `type`.map(_.toLowerCase).contains("noun") || nounRegex.matches(word) || genderOpt.nonEmpty

    lazy val genderOpt: Option[Noun.Gender] = {
      import Noun.Gender._
      val wordArt = word.trim.take(3)
      if(wordArt == "der") Some(M)
      else if(wordArt == "die") Some(F)
      else if(wordArt == "das") Some(N)
      else gender.map(_.head.toLower) match {
        case Some('m') => Some(M)
        case Some('n') => Some(N)
        case Some('f') => Some(F)
        case _ => None
      }
    }

    lazy val actualWord: String = word.toLowerCase match {
      case JsonIn.nounRegex(_, w) => w.trim.capitalize
      case w if isNoun => w.trim.capitalize
      case w => w
    }
  }

  object JsonIn {
    implicit def encoder[F[_]]: EntityEncoder[F, JsonIn] = dropJsonEnc
    implicit def decoder[F[_] : Concurrent]: EntityDecoder[F, JsonIn] = jsonOf

    val nounRegex = "(der|die|das)\\s+(.+)".r
  }
}