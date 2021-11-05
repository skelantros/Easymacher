package ru.skelantros.easymacher

package object db {
  type DbResult[A] = Either[Error, A]
  type DbUnit = DbResult[Unit]

  object DbResult {
    def of[A](x: A): DbResult[A] = Right(x)
    val unit: DbUnit = Right(())
    def mistake[A](msg: String): DbResult[A] = Left(Error.mistake(msg))
    def thr[A](t: Throwable): DbResult[A] = Left(Error.thr(t))
  }

  sealed trait Error {
    def map[A](fMis: String => A, fThr: Throwable => A): A = this match {
      case Mistake(msg) => fMis(msg)
      case Thr(t) => fThr(t)
    }
  }
  case class Mistake(msg: String) extends Error
  case class Thr(t: Throwable) extends Error

  object Error {
    def mistake(msg: String): Error = Mistake(msg)
    def thr(t: Throwable): Error = Thr(t)
  }
}
