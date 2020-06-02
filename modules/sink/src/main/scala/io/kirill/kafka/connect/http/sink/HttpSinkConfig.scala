package io.kirill.kafka.connect.http.sink

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.connect.sink.SinkTask

object HttpSinkConfig {
  val HTTP_API_URL     = "http.api.url"
  val HTTP_API_URL_DOC = "HTTP API URL"

  val HTTP_REQUEST_METHOD         = "http.request.method"
  val HTTP_REQUEST_METHOD_DOC     = "HTTP Request Method"
  val HTTP_REQUEST_METHOD_DEFAULT = "POST"

  val MAX_RETRIES         = "max.retries"
  val MAX_RETRIES_DEFAULT = 10
  val MAX_RETRIES_DOC     = "The maximum number of times to retry on errors before failing the task"

  val TOPICS     = SinkTask.TOPICS_CONFIG
  val TOPICS_DOC = "The Kafka topic to read from."

  val config: ConfigDef = new ConfigDef()
    .define(
      HTTP_API_URL,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      HTTP_API_URL_DOC
    )
    .define(
      HTTP_REQUEST_METHOD,
      ConfigDef.Type.STRING,
      HTTP_REQUEST_METHOD_DEFAULT,
      ConfigDef.Importance.HIGH,
      HTTP_REQUEST_METHOD_DOC
    )
    .define(
      MAX_RETRIES,
      ConfigDef.Type.INT,
      MAX_RETRIES_DEFAULT,
      ConfigDef.Importance.MEDIUM,
      MAX_RETRIES_DOC
    )
    .define(TOPICS, Type.LIST, Importance.HIGH, TOPICS_DOC)
}
