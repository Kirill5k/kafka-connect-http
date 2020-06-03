package io.kirill.kafka.connect.http.sink

import org.slf4j.LoggerFactory

trait Logging {
  val loggerName = this.getClass.getName
  @transient lazy val logger = LoggerFactory.getLogger(loggerName)
}
