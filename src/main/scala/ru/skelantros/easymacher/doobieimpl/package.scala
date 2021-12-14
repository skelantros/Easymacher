package ru.skelantros.easymacher

import java.util.UUID

import cats.Monad
import cats.effect.{Async, MonadCancelThrow}
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import ru.skelantros.easymacher.db.{DbResult, DbUnit, Mistake, Thr}
import cats.implicits._
import doobie.implicits._
import doobie.util.update.Update0

// TODO унифицировать вспомогательные методы этого пакета
package object doobieimpl {
  object PsqlStates {
    import doobie.postgres._
    val uniqueViolation = sqlstate.class23.UNIQUE_VIOLATION
    val foreignKeyViolation = sqlstate.class23.FOREIGN_KEY_VIOLATION
  }

  def processSelect[F[_] : MonadCancelThrow, A, B](query: ConnectionIO[A])(f: A => B)(implicit xa: Transactor[F]): F[DbResult[B]] =
    for {
      fromDb <- query.transact(xa).attempt
      res = fromDb match {
        case Right(a) => DbResult.of(f(a))
        case Left(t) => DbResult.thr(t)
      }
    } yield res

  def processSelectEither[F[_] : MonadCancelThrow, A, B](query: ConnectionIO[Either[String, A]])(f: A => B)(implicit xa: Transactor[F]): F[DbResult[B]] =
    for {
      fromDb <- query.transact(xa).attempt
      res = fromDb match {
        case Right(Right(a)) => DbResult.of(f(a))
        case Right(Left(msg)) => DbResult.mistake(msg)
        case Left(t) => DbResult.thr(t)
      }
    } yield res

  def processSelectUnitEither[F[_] : MonadCancelThrow, A](query: ConnectionIO[Either[String, A]])(implicit xa: Transactor[F]): F[DbUnit] =
    for {
      fromDb <- query.transact(xa).attempt
      res = fromDb match {
        case Right(Right(_)) => DbResult.unit
        case Right(Left(msg)) => DbResult.mistake(msg)
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

  def processUpdate[F[_] : MonadCancelThrow](query: Update0)(implicit xa: Transactor[F]): F[DbUnit] =
    query.run.attempt.transact(xa).map {
      case Right(_) => DbResult.unit
      case Left(t) => DbUnit.thr(t)
    }
}
