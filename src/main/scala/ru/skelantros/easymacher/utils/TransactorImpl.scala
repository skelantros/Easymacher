package ru.skelantros.easymacher.utils

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

object TransactorImpl {
  // TODO переписать реализацию транзактора таким образом, чтобы она подхватывала значения параметров из среды (пока что необязательно)
  def apply[F[_] : Async]: Transactor[F] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:easymacher",
    "doobie",
    "123"
  )
}
