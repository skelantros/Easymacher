package ru.skelantros.easymacher.utils

case class Email private[utils] (name: String, host: String) {
  def asString: String = s"$name@$host"
}

object Email {
  private val emailReg = """(([A-z0-9][A-z0-9._]*[A-z][A-z0-9._]*)|([A-z]))@(([A-z0-9]+\.[A-z])*[A-z0-9]+)""".r
  private val nicknameReg = """([A-z0-9][A-z0-9._]*[A-z][A-z0-9._]*)|([A-z])""".r
  private val hostReg = """([A-z0-9]+\.[A-z])*[A-z0-9]+""".r

  def apply(name: String, host: String): Option[Email] =
    (Some(name).filter(nicknameReg.matches) zip Some(host).filter(hostReg.matches)).map(t => new Email(t._1, t._2))

  def apply(str: String): Option[Email] = str match {
    case emailReg(name, _, _, host, _*) => Some(new Email(name, host))
    case _ => None
  }
}