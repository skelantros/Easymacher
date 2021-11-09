package ru.skelantros.easymacher

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.http4s._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.skelantros.easymacher.entities.{Role, User}
import ru.skelantros.easymacher.utils.Email

trait CommonSpec extends AnyFlatSpec with Matchers {
  implicit val runtime: IORuntime = IORuntime.global

  def check[A](response: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])
              (implicit dec: EntityDecoder[IO, A]): Unit = {
    val resp = response.unsafeRunSync()
    expectedStatus shouldBe resp.status
    expectedBody.fold(
      resp.body.compile.toVector.unsafeRunSync().isEmpty shouldBe true
    )(expected => resp.as[A].unsafeRunSync() shouldBe expected)
  }

  val usersSample = Seq(
    User(1, "skelantros", Email("skelantros@easymacher.ru").get, "23052001", Role.Admin, true, "001", Some("Alex"), Some("Egorowski")),
    User(2, "adefful", Email("ad3fful@easymacher.ru").get, "1234", Role.User, true, "002", Some("Alex"), None),
    User(3, "g03th3", Email("g03th3@klassik.de").get, "5678", Role.User, true, "003", None, None),
    User(4, "damned", Email("damned@mail.ru").get, "xd", Role.User, false, "004", None, None)
  )
}
