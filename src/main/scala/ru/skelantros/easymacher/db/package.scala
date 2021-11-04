package ru.skelantros.easymacher

package object db {
  type DbResult[A] = Either[Error, A]
  type DbUnit = DbResult[Unit]

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
