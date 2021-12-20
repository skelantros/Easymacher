package ru.skelantros.easymacher.utils

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

object TransactorImpl {
  private def dbName = System.getenv("EASYMACHER_DB_NAME")
  private def dbUser = System.getenv("EASYMACHER_DB_USER")
  private def dbPassword = System.getenv("EASYMACHER_DB_PASSWORD")
  private def dbPort = System.getenv("EASYMACHER_DB_PORT")
  private def dbAddress = System.getenv("EASYMACHER_DB_ADDRESS")

  def apply[F[_] : Async]: Transactor[F] = {
    println(dbName)
    println(dbUser)
    println(dbPassword)
    println(dbPort)
    println(dbAddress)

    Transactor.fromDriverManager[F](
      "org.postgresql.Driver",
      s"jdbc:postgresql://$dbAddress:$dbPort/$dbName",
      dbUser,
      dbPassword
    )
  }
}
