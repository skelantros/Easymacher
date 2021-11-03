package ru.skelantros.easymacher.db

sealed trait DbUnit extends Product with Serializable

object DbUnit {
  private case object Success extends DbUnit
  private case class Thr(t: Throwable) extends DbUnit
  private case class UserMistake(msg: String) extends DbUnit

  val notImplemented: DbUnit = Thr(new NotImplementedError)
  val success: DbUnit = Success
  def thr(t: Throwable): DbUnit = Thr(t)
  def mistake(msg: String): DbUnit = UserMistake(msg)
}