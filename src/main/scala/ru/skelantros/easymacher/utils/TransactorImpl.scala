package ru.skelantros.easymacher.utils

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

object TransactorImpl {
  private def dbName = System.getenv("EASYMACHER_DB_NAME")
  private def dbUser = System.getenv("EASYMACHER_DB_USER")
  private def dbPassword = System.getenv("EASYMACHER_DB_PASSWORD")
  private def dbAddress = System.getenv("DATABASE_URL")

  def apply[F[_] : Async]: Transactor[F] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    s"jdbc:postgresql:$dbAddress",
    dbUser,
    dbPassword
  )
}
