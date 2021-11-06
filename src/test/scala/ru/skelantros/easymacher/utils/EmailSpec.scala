package ru.skelantros.easymacher.utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EmailSpec extends AnyFlatSpec with Matchers {
  val correctEmails = Map(
    "skelantros@gmail.com" -> new Email("skelantros", "gmail.com"),
    "test@wat.com.ru" -> new Email("test", "wat.com.ru"),
    "whynot_228@hello19.com" -> new Email("whynot_228", "hello19.com")
  )

  val incorrectEmails = Seq(
    "1@test.ru",
    ".hello@wat.ru",
    "warum@test..ru",
    "hell@.test.ru"
  )

  "Email" should "parse correct emails correctly" in {
    for((email, res) <- correctEmails)
      Email(email) shouldBe Some(res)
  }

  it should "not parse incorrect emails" in {
    for(email <- incorrectEmails)
      Email(email) shouldBe None
  }
}
