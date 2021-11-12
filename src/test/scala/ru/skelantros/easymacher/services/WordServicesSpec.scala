package ru.skelantros.easymacher.services

import ru.skelantros.easymacher.CommonSpec
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import ru.skelantros.easymacher.db.{UserDb, UserMock, WordDb, WordMock}
import ru.skelantros.easymacher.entities.Word
import ru.skelantros.easymacher.services.WordServices.JsonOut
import ru.skelantros.easymacher.utils.StatusMessages._

class WordServicesSpec extends CommonSpec {
  val userDb: UserDb.Select[IO] = new UserMock(usersSample)

  type WordMockDb = WordDb.Select[IO] with WordDb.AddAny[IO] with WordDb.AddNoun[IO]
  def wordsDb(init: Seq[Word]): WordMockDb = new WordMock(init, userDb)
  def wordsDb: WordMockDb = wordsDb(wordsSample)

  val services = new WordServices[IO]

  implicit val outSeq: EntityDecoder[IO, Seq[JsonOut]] = jsonOf
  implicit val outOpt: EntityDecoder[IO, Option[JsonOut]] = jsonOf

  val firstWordJson = Json.obj(
    "id" := 1,
    "word" := "machen",
    "translate" := "делать",
    "owner" := 1,
    "type" := "unknown"
  )

  val fifthWordJson = Json.obj(
    "id" := 5,
    "word" := "Fenster",
    "plural" := "Fenster",
    "gender" := "n",
    "translate" := "окно",
    "owner" := 1,
    "type" := "noun"
  )


  "all" should "return all words in db" in {
    implicit val db = wordsDb
    val req = Request[IO](method = Method.GET, uri=uri"/all-words")
    val actualResp = services.all.orNotFound.run(req)
    check(actualResp, Status.Ok, Some(wordsSample.map(JsonOut.fromWord)))
  }

  "byId" should "return existing word" in {
    implicit val db = wordsDb
    val req = Request[IO](method = Method.GET, uri=uri"/all-words?id=1")
    val actualResp = services.byId.orNotFound.run(req)
    check(actualResp, Status.Ok, Some(firstWordJson))
  }

  it should "return nouns with corresponding info" in {
    implicit val db = wordsDb
    val req = Request[IO](method = Method.GET, uri=uri"/all-words?id=5")
    val actualResp = services.byId.orNotFound.run(req)
    check(actualResp, Status.Ok, Some(fifthWordJson))
  }

  it should "not return non-existing words" in {
    implicit val db = wordsDb
    val req = Request[IO](method = Method.GET, uri=uri"/all-words?id=7")
    val actualResp = services.byId.orNotFound.run(req)
    check(actualResp, Status.BadRequest, Some("Word with id 7 does not exist."))
  }

  "ofUser" should "return all words to user" in {
    implicit val db = wordsDb
    val req = Request[IO](method = Method.GET, uri=uri"/words")
    val actualResp = services.ofUser(skelantros).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(wordsSample.filter(_.owner == skelantros).map(JsonOut.fromWord)))
  }

  "ofUserById" should "not return words of other users" in {
    implicit val db = wordsDb
    val req = Request[IO](method = Method.GET, uri=uri"/words?id=4")
    val actualResp = services.ofUserById(skelantros).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Some(noPermission))
  }

  "add-word" should "add words without type to the database (from user)" in {
    implicit val db = wordsDb
    val word = Json.obj(
      "word" := "suchen",
      "translate" := "искать"
    )

    val req = Request[IO](method = Method.POST, uri=uri"/add-word").withEntity(word)
    val actualResp = services.addWord(skelantros).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(()))

    val wordSaved = Json.obj(
      "word" := "suchen",
      "translate" := "искать",
      "type" := "unknown",
      "id" := 7,
      "owner" := 1
    )

    val req2 = Request[IO](method = Method.GET, uri=uri"/words?id=7")
    val actualResp2 = services.ofUserById(skelantros).orNotFound.run(req2)
    check(actualResp2, Status.Ok, Some(wordSaved))
  }

  val arztJson = Json.obj(
    "word" := "Arzt",
    "translate" := "врач",
    "type" := "noun",
    "gender" := "m",
    "id" := 7,
    "owner" := 1
  )

  it should "add nouns (with explicit gender) to the database (from user)" in {
    implicit val db = wordsDb
    val word = Json.obj(
      "word" := "arzt",
      "translate" := "врач",
      "gender" := "m"
    )

    val req = Request[IO](method = Method.POST, uri=uri"/add-word").withEntity(word)
    val actualResp = services.addWord(skelantros).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(()))

    val req2 = Request[IO](method = Method.GET, uri=uri"/words?id=7")
    val actualResp2 = services.ofUserById(skelantros).orNotFound.run(req2)
    check(actualResp2, Status.Ok, Some(arztJson))
  }

  it should "add nouns (with article) to the database (from user)" in {
    implicit val db = wordsDb
    val word = Json.obj(
      "word" := "der arzt",
      "translate" := "врач"
    )

    val req = Request[IO](method = Method.POST, uri=uri"/add-word").withEntity(word)
    val actualResp = services.addWord(skelantros).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(()))

    val req2 = Request[IO](method = Method.GET, uri=uri"/words?id=7")
    val actualResp2 = services.ofUserById(skelantros).orNotFound.run(req2)
    check(actualResp2, Status.Ok, Some(arztJson))
  }

  it should "not add nouns without gender or article (from user)" in {
    implicit val db = wordsDb
    val word = Json.obj(
      "word" := "Arzt",
      "translate" := "врач",
      "type" := "noun"
    )

    val req = Request[IO](method = Method.POST, uri=uri"/add-word").withEntity(word)
    val actualResp = services.addWord(skelantros).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Some(noGenderNoun))

    val req2 = Request[IO](method = Method.GET, uri=uri"/words?id=7")
    val actualResp2 = services.ofUserById(skelantros).orNotFound.run(req2)
    check(actualResp2, Status.BadRequest, Some("Word with id 7 does not exist."))
  }
}
