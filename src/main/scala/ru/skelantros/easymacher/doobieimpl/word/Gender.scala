package ru.skelantros.easymacher.doobieimpl.word

sealed abstract class Gender(val value: String)
object Gender {
  def apply(str: String): Gender = str match {
    case "m" => M
    case "n" => N
    case "f" => F
  }

  case object M extends Gender("m")
  case object F extends Gender("f")
  case object N extends Gender("n")
}