package ru.skelantros.easymacher.entities

sealed trait DbResult extends Product with Serializable

object DbResult {
  private case object Success extends DbResult
  private case class Thr(t: Throwable) extends DbResult
  private case class UserMistake(msg: String) extends DbResult

  val notImplemented: DbResult = Thr(new NotImplementedError)
  val success: DbResult = Success
  def thr(t: Throwable): DbResult = Thr(t)
  def mistake(msg: String): DbResult = UserMistake(msg)
}