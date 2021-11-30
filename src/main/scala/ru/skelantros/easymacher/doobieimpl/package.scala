package ru.skelantros.easymacher

import java.util.UUID

import cats.Monad
import cats.effect.{Async, MonadCancelThrow}
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.{DbResult, Mistake, Thr}
import cats.implicits._
import doobie.implicits._

package object doobieimpl {
  def processSelect[F[_] : MonadCancelThrow, A, B](query: ConnectionIO[A])(f: A => B)(implicit xa: Transactor[F]): F[DbResult[B]] =
    for {
      fromDb <- query.transact(xa).attempt
      res = fromDb match {
        case Right(a) => DbResult.of(f(a))
        case Left(t) => DbResult.thr(t)
      }
    } yield res

  def processOptSelect[F[_] : MonadCancelThrow, A, B](query: ConnectionIO[Option[A]])(f: A => B, mistakeMsg: => String)(implicit xa: Transactor[F]): F[DbResult[B]] =
    for {
      optRes <- processSelect[F, Option[A], Option[B]](query)(_.map(f))
      res = optRes match {
        case Right(Some(b)) => DbResult.of(b)
        case Right(None) => DbResult.mistake(mistakeMsg)
        case Left(Mistake(msg)) => DbResult.mistake(msg)
        case Left(Thr(t)) => DbResult.thr(t)
      }
    } yield res
}
