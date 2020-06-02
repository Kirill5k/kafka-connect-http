package io.kirill.kafka.connect.http.sink

import java.util

import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}

class HttpSinkConfig(
    val props: util.Map[String, String]
) extends AbstractConfig(HttpSinkConfig.DEF, props) {
  val httpApiUrl        = getString(HttpSinkConfig.HTTP_API_URL)
  val httpRequestMethod = getString(HttpSinkConfig.HTTP_REQUEST_METHOD)
  val batchSize         = getInt(HttpSinkConfig.BATCH_SIZE)
  val maxRetries        = getInt(HttpSinkConfig.MAX_RETRIES)
}

object HttpSinkConfig {
  val HTTP_API_URL     = "http.api.url"
  val HTTP_API_URL_DOC = "Http api url where the data will be sent"

  val HTTP_REQUEST_METHOD         = "http.request.method"
  val HTTP_REQUEST_METHOD_DOC     = "HTTP Request Method"
  val HTTP_REQUEST_METHOD_DEFAULT = "POST"

  val BATCH_SIZE         = "batch.size"
  val BATCH_SIZE_DOC     = "The number of records accumulated in a batch before the HTTP API will be invoked"
  val BATCH_SIZE_DEFAULT = 1

  val MAX_RETRIES         = "max.retries"
  val MAX_RETRIES_DOC     = "The maximum number of times to retry on errors before failing the task"
  val MAX_RETRIES_DEFAULT = 10

  val DEF: ConfigDef = new ConfigDef()
    .define(HTTP_API_URL, Type.STRING, ConfigDef.NO_DEFAULT_VALUE, Importance.HIGH, HTTP_API_URL_DOC)
    .define(HTTP_REQUEST_METHOD, Type.STRING, HTTP_REQUEST_METHOD_DEFAULT, Importance.HIGH, HTTP_REQUEST_METHOD_DOC)
    .define(MAX_RETRIES, Type.INT, MAX_RETRIES_DEFAULT, Importance.MEDIUM, MAX_RETRIES_DOC)
    .define(BATCH_SIZE, Type.INT, BATCH_SIZE_DEFAULT, Importance.MEDIUM, BATCH_SIZE_DOC)
}
