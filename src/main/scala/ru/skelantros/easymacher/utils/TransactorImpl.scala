package ru.skelantros.easymacher.utils

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

object TransactorImpl {
  private def dbName = System.getenv("EASYMACHER_DB_NAME")
  private def dbUser = System.getenv("EASYMACHER_DB_USER")
  private def dbPassword = System.getenv("EASYMACHER_DB_PASSWORD")
  private def dbAddress = System.getenv("DATABASE_URL")

  private def dbHerokuAddress: HerokuDbAddress = HerokuDbAddress(System.getenv("DATABASE_URL"))

  case class HerokuDbAddress(name: String, password: String, address: String, port: String, database: String)
  object HerokuDbAddress {
    val regex = """postgres://([^\:]+):([^\:]+)@([^\:]+):([0-9]+)/([^\:]+)""".r
    def apply(str: String): HerokuDbAddress = str match {
      case regex(name, password, address, port, database) => HerokuDbAddress(name, password, address, port, database)
    }
  }

  def apply[F[_] : Async]: Transactor[F] = {
    val HerokuDbAddress(name, password, address, port, database) = dbHerokuAddress

    Transactor.fromDriverManager[F](
      "org.postgresql.Driver",
      s"jdbc:postgresql://$address:$port/$database",
      name,
      password
    )
  }
}
