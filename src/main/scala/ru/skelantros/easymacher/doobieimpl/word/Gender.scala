package ru.skelantros.easymacher.doobieimpl.word

import ru.skelantros.easymacher.entities.Noun.{Gender => EntityGender}

sealed abstract class Gender(val value: String, val entityGender: EntityGender)
object Gender {
  def apply(str: String): Gender = str match {
    case "m" => M
    case "n" => N
    case "f" => F
  }

  def apply(g: EntityGender): Gender = g match {
    case EntityGender.M => M
    case EntityGender.F => F
    case EntityGender.N => N
  }

  case object M extends Gender("m", EntityGender.M)
  case object F extends Gender("f", EntityGender.F)
  case object N extends Gender("n", EntityGender.N)
}