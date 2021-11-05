package ru.skelantros.easymacher

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.http4s._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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
}
