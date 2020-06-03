package io.kirill.kafka.connect.http.sink

import java.util

import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}

class HttpSinkConfig(
    val props: util.Map[String, String]
) extends AbstractConfig(HttpSinkConfig.DEF, props) {
  val httpApiUrl: String        = getString(HttpSinkConfig.HTTP_API_URL)
  val httpRequestMethod: String = getString(HttpSinkConfig.HTTP_REQUEST_METHOD)
  val batchSize: Int            = getInt(HttpSinkConfig.BATCH_SIZE)
  val maxRetries: Int           = getInt(HttpSinkConfig.MAX_RETRIES)

  val avroConverterConf: Map[String, String] = Map(
    "schema.registry.url"          -> props.get("value.converter.schema.registry.url"),
    "enhanced.avro.schema.support" -> props.get("enhanced.avro.schema.support"),
    "specific.avro.reader"         -> props.get("specific.avro.reader")
  )
}

object HttpSinkConfig {
  val HTTP_API_URL     = "http.api.url"
  val HTTP_API_URL_DOC = "Http api url where the data will be sent"

  val HTTP_REQUEST_METHOD         = "http.request.method"
  val HTTP_REQUEST_METHOD_DOC     = "HTTP Request Method"
  val HTTP_REQUEST_METHOD_DEFAULT = "POST"

  val HTTP_HEADERS         = "http.headers"
  val HTTP_HEADERS_DOC     = "HTTP headers to be included in all requests separated by the header.separator"
  val HTTP_HEADERS_DEFAULT = ""

  val HEADERS_SEPARATOR         = "headers.separator"
  val HEADERS_SEPARATOR_DOC     = "Separator character used in headers property."
  val HEADERS_SEPARATOR_DEFAULT = "|"

  val BATCH_SIZE         = "batch.size"
  val BATCH_SIZE_DOC     = "The number of records accumulated in a batch before the HTTP API will be invoked"
  val BATCH_SIZE_DEFAULT = 1

  val BATCH_PREFIX         = "batch.prefix"
  val BATCH_PREFIX_DEFAULT = ""
  val BATCH_PREFIX_DOC =
    "prefix added to record batches that will be applied once at the beginning of the batch of records"

  val BATCH_SUFFIX         = "batch.suffix"
  val BATCH_SUFFIX_DEFAULT = ""
  val BATCH_SUFFIX_DOC     = "suffix added to record batches that will be applied once at the end of the batch of records"

  val BATCH_SEPARATOR         = "batch.seperator"
  val BATCH_SEPARATOR_DEFAULT = ","
  val BATCH_SEPARATOR_DOC     = "seperator for records in a batch"

  val MAX_RETRIES         = "max.retries"
  val MAX_RETRIES_DOC     = "The maximum number of times to retry on errors before failing the task"
  val MAX_RETRIES_DEFAULT = 10

  val RETRY_BACKOFF         = "retry.backoff.ms"
  val RETRY_BACKOFF_DOC     = "The duration in milliseconds to wait after an error before a retry attempt is made"
  val RETRY_BACKOFF_DEFAULT = 3000

  val REGEX_PATTERNS         = "regex.patterns"
  val REGEX_PATTERNS_DOC     = "character seperated regex patterns to match for replacement in the destination messages"
  val REGEX_PATTERNS_DEFAULT = ""

  val REGEX_REPLACEMENTS = "regex.replacements"
  val REGEX_REPLACEMENTS_DOC =
    "character seperated regex replacements to use with the patterns in regex.patterns. ${key} and ${topic} can be used here."
  val REGEX_REPLACEMENTS_DEFAULT = ""

  val REGEX_SEPARATOR         = "regex.separator"
  val REGEX_SEPARATOR_DOC     = "separator character used in regex.patterns and regex.replacements property."
  val REGEX_SEPARATOR_DEFAULT = "~"

  val DEF: ConfigDef = new ConfigDef()
    .define(HTTP_API_URL, Type.STRING, ConfigDef.NO_DEFAULT_VALUE, Importance.HIGH, HTTP_API_URL_DOC)
    .define(HTTP_REQUEST_METHOD, Type.STRING, HTTP_REQUEST_METHOD_DEFAULT, Importance.HIGH, HTTP_REQUEST_METHOD_DOC)
    .define(HTTP_HEADERS, Type.STRING, HTTP_HEADERS_DEFAULT, Importance.LOW, HTTP_HEADERS_DOC)
    .define(BATCH_SIZE, Type.INT, BATCH_SIZE_DEFAULT, Importance.MEDIUM, BATCH_SIZE_DOC)
    .define(BATCH_PREFIX, Type.STRING, BATCH_PREFIX_DEFAULT, Importance.MEDIUM, BATCH_PREFIX_DOC)
    .define(BATCH_SUFFIX, Type.STRING, BATCH_SUFFIX_DEFAULT, Importance.MEDIUM, BATCH_SUFFIX_DOC)
    .define(BATCH_SEPARATOR, Type.STRING, BATCH_SEPARATOR_DEFAULT, Importance.MEDIUM, BATCH_SEPARATOR_DOC)
    .define(HEADERS_SEPARATOR, Type.STRING, HEADERS_SEPARATOR_DEFAULT, Importance.MEDIUM, HEADERS_SEPARATOR_DOC)
    .define(REGEX_PATTERNS, Type.STRING, REGEX_PATTERNS_DEFAULT, Importance.LOW, REGEX_PATTERNS_DOC)
    .define(REGEX_REPLACEMENTS, Type.STRING, REGEX_REPLACEMENTS_DEFAULT, Importance.LOW, REGEX_REPLACEMENTS_DOC)
    .define(REGEX_SEPARATOR, Type.STRING, REGEX_SEPARATOR_DEFAULT, Importance.MEDIUM, REGEX_SEPARATOR_DOC)
    .define(MAX_RETRIES, Type.INT, MAX_RETRIES_DEFAULT, Importance.MEDIUM, MAX_RETRIES_DOC)
    .define(RETRY_BACKOFF, Type.LONG, RETRY_BACKOFF_DEFAULT, Importance.MEDIUM, RETRY_BACKOFF_DOC)
}
