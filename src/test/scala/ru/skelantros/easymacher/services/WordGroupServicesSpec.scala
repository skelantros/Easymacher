package ru.skelantros.easymacher.services

import cats.effect.IO
import cats.implicits._
import io.circe.Json
import io.circe.syntax._
import org.http4s.{EntityDecoder, Method, Request, Status}
import ru.skelantros.easymacher.{CommonSpec}
import ru.skelantros.easymacher.db.{UserMock, WordGroupMock, WordMock}
import ru.skelantros.easymacher.services.WordServices.{JsonOut => WordOut}
import org.http4s.implicits._
import org.http4s.circe._
import io.circe.generic.auto._
import ru.skelantros.easymacher.utils.StatusMessages._

class WordGroupServicesSpec extends CommonSpec {
  val machenJson = Json.obj(
    "id" := 1,
    "word" := "machen",
    "translate" := "делать",
    "owner" := 1,
    "type" := "unknown"
  )

  val arbeitenJson = Json.obj(
    "id" := 2,
    "word" := "arbeiten",
    "owner" := 1,
    "type" := "unknown"
  )

  val fensterJson = Json.obj(
    "id" := 5,
    "word" := "Fenster",
    "plural" := "Fenster",
    "gender" := "n",
    "translate" := "окно",
    "owner" := 1,
    "type" := "noun"
  )

  val verbsJson = Json.obj(
    "id" := 1,
    "name" := "verbs",
    "owner" := 1,
    "isShared" := false
  )
  val nounsJson = Json.obj(
    "id" := 2,
    "name" := "nouns",
    "owner" := 1,
    "isShared" := true
  )

  val userDb = new UserMock[IO](usersSample)
  val wordDb = new WordMock[IO](wordsSample, userDb)
  def groupsMock = new WordGroupMock[IO](wordGroupsSample, userDb, wordDb)

  implicit val jsonSeqDecoder: EntityDecoder[IO, Seq[Json]] = jsonOf

  implicit val wordOutSeqDecoder: EntityDecoder[IO, Seq[WordOut]] = jsonOf

  val services = new WordGroupServices[IO]
  val allRequest = Request[IO](method = Method.GET, uri = uri"/word-group/all")

  "allVisible" should "return all own groups" in {
    implicit val db = groupsMock
    val actualResp = services.allVisible(skelantros).orNotFound.run(allRequest)
    check(actualResp, Status.Ok, Some(Seq(verbsJson, nounsJson)))
  }

  it should "return only shared groups (of user skelantros)" in {
    implicit val db = groupsMock
    val actualResp = services.allVisible(adefful).orNotFound.run(allRequest)
    check(actualResp, Status.Ok, Some(Seq(nounsJson)))
  }

  "byIdVisible" should "return visible group" in {
    val req = Request[IO](method = Method.GET, uri = uri"/word-group/2")
    implicit val db = groupsMock
    val actualResp = services.byIdVisible(adefful).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(nounsJson))
  }

  it should "not return non-visible groups" in {
    val req = Request[IO](method = Method.GET, uri = uri"/word-group/1")
    implicit val db = groupsMock
    val actualResp = services.byIdVisible(adefful).orNotFound.run(req)
    check(actualResp, Status.Forbidden, Some(noAccessToGroup(1)))
  }

  it should "not return non-existing group" in {
    val req = Request[IO](method = Method.GET, uri = uri"/word-group/3")
    implicit val db = groupsMock
    val actualResp = services.byIdVisible(adefful).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Some(noGroupById(3)))
  }

  "wordsOfGroup" should "return words of visible group" in {
    val req = Request[IO](method = Method.GET, uri = uri"/word-group/2/words")
    implicit val db = groupsMock
    val actualResp = services.wordsOfGroup(adefful).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(Seq(stuhl, fenster, flur).map(WordOut.fromWord)))
  }

  it should "not return words of non visible group" in {
    val req = Request[IO](method = Method.GET, uri = uri"/word-group/1/words")
    implicit val db = groupsMock
    val actualResp = services.wordsOfGroup(adefful).orNotFound.run(req)
    check(actualResp, Status.Forbidden, Some(noAccessToGroup(1)))
  }

  it should "not return words of non existing group" in {
    val req = Request[IO](method = Method.GET, uri = uri"/word-group/3/words")
    implicit val db = groupsMock
    val actualResp = services.wordsOfGroup(adefful).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Some(noGroupById(3)))
  }

  "create" should "create group" in {
    implicit val db = groupsMock
    val body = Json.obj(
      "name" := "something",
      "isShared" := true
    )

    val result = Json.obj(
      "name" := "something",
      "id" := 3,
      "owner" := 1,
      "isShared" := true
    )

    val req = Request[IO](method = Method.POST, uri=uri"/word-group").withEntity(body)
    val actualResp = services.create(skelantros).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(result))

    val selectReq = Request[IO](method = Method.GET, uri=uri"/word-group/3")
    val selectResp = services.byIdVisible(skelantros).orNotFound.run(selectReq)
    check(selectResp, Status.Ok, Some(result))
  }

  "addWords" should "add words to user's group" in {
    implicit val db = groupsMock

    val body = Json.obj(
      "words" := Seq(5)
    )
    val req = Request[IO](method = Method.PATCH, uri=uri"/word-group/1/words").withEntity(body)
    val actualResp = services.addWords(skelantros).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(()))

    val words = Json.arr(
      machenJson,
      arbeitenJson,
      fensterJson
    )

    val selectReq = Request[IO](method = Method.GET, uri=uri"/word-group/1/words")
    val selectResp = services.wordsOfGroup(skelantros).orNotFound.run(selectReq)
    check(selectResp, Status.Ok, Some(words))
  }

  it should "deny adding words to not user's group" in {
    implicit val db = groupsMock

    val body = Json.obj(
      "words" := Seq(5)
    )
    val req = Request[IO](method = Method.PATCH, uri=uri"/word-group/1/words").withEntity(body)
    val actualResp = services.addWords(adefful).orNotFound.run(req)
    check(actualResp, Status.Forbidden, Some(cannotEditGroup(1)))
  }

  it should "do nothing when adding to non-existing group" in {
    implicit val db = groupsMock

    val body = Json.obj(
      "words" := Seq(5)
    )
    val req = Request[IO](method = Method.PATCH, uri=uri"/word-group/4/words").withEntity(body)
    val actualResp = services.addWords(adefful).orNotFound.run(req)
    check(actualResp, Status.BadRequest, Some(noGroupById(4)))
  }

  "update" should "update group of user" in {
    implicit val db = groupsMock

    val body = Json.obj(
      "name" := "watahell"
    )

    val newJson = Json.obj(
      "id" := 1,
      "name" := "watahell",
      "owner" := 1,
      "isShared" := false
    )

    val req = Request[IO](method = Method.PATCH, uri=uri"/word-group/1").withEntity(body)
    val actualResp = services.update(skelantros).orNotFound.run(req)
    check(actualResp, Status.Ok, Some(newJson))

    val selectReq = Request[IO](method = Method.GET, uri=uri"/word-group/1")
    val selectResp = services.byIdVisible(skelantros).orNotFound.run(selectReq)
    check(selectResp, Status.Ok, Some(newJson))
  }

  it should "not access changing of other user's group" in {
    implicit val db = groupsMock

    val body = Json.obj(
      "name" := "watahell"
    )
    val req = Request[IO](method = Method.PATCH, uri=uri"/word-group/1").withEntity(body)
    val actualResp = services.update(adefful).orNotFound.run(req)
    check(actualResp, Status.Forbidden, Some(cannotEditGroup(1)))
  }
}
