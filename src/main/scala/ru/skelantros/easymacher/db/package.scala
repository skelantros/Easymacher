package ru.skelantros.easymacher

package object db {
  type DbResult[A] = Either[Error, A]
  type UnitResult = DbResult[Unit]

  sealed trait Error
  case class Mistake(msg: String) extends Error
  case class Thr(t: Throwable) extends Error

  object Error {
    def mistake(msg: String): Error = Mistake(msg)
    def thr(t: Throwable): Error = Thr(t)
  }
}
