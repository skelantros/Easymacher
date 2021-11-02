package ru.skelantros.easymacher.entities

sealed trait DbResult

object DbResult {
  case object Success extends DbResult
  case class Error(t: Throwable) extends DbResult

  val notImplemented: DbResult = Error(new NotImplementedError)
}