package ru.skelantros.easymacher.doobieimpl

import doobie.util.log.LogHandler

trait DoobieLogging {
  implicit val log = LogHandler.jdkLogHandler
}
