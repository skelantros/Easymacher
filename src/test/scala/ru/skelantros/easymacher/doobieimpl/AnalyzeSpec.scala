package ru.skelantros.easymacher.doobieimpl

import cats.effect.IO
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import ru.skelantros.easymacher.utils.TransactorImpl

trait AnalyzeSpec extends AnyFunSuite with IOChecker {
  override def transactor: doobie.Transactor[IO] = TransactorImpl[IO]
}
