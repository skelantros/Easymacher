package ru.skelantros.easymacher

package object db {
  type DbResult[+A] = Either[DbError, A]
  type DbUnit = DbResult[Unit]

  object DbResult {
    def of[A](x: A): DbResult[A] = Right(x)
    val unit: DbUnit = Right(())
    def mistake[A](msg: String): DbResult[A] = Left(DbError.mistake(msg))
    def thr[A](t: Throwable): DbResult[A] = Left(DbError.thr(t))
  }

  object DbUnit {
    def mistake(msg: String): DbUnit = Left(DbError.mistake(msg))
    def thr(t: Throwable): DbUnit = Left(DbError.thr(t))
  }

  sealed trait DbError {
    def map[A](fMis: String => A, fThr: Throwable => A): A = this match {
      case Mistake(msg) => fMis(msg)
      case Thr(t) => fThr(t)
    }
  }
  case class Mistake(msg: String) extends DbError
  case class Thr(t: Throwable) extends DbError

  object DbError {
    def mistake(msg: String): DbError = Mistake(msg)
    def thr(t: Throwable): DbError = Thr(t)
  }
}
