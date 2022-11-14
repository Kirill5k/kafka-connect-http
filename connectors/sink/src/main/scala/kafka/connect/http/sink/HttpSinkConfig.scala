/*
 * Copyright 2020 Kafka Connect Http
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.connect.http.sink

import java.util

import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}

import scala.jdk.CollectionConverters._

class HttpSinkConfig(
    val props: util.Map[String, String]
) extends AbstractConfig(HttpSinkConfig.DEF, props) {
  import HttpSinkConfig._

  private val headerSep = getString(HEADERS_SEPARATOR)
  private val regexSep  = getString(REGEX_SEPARATOR)

  val httpApiUrl: String        = getString(HTTP_API_URL)
  val httpRequestMethod: String = getString(HTTP_REQUEST_METHOD)
  val httpHeaders: Map[String, String] = getString(HTTP_HEADERS)
    .split(headerSep)
    .filter(_.nonEmpty)
    .toList
    .map(_.split(":"))
    .map(x => (x(0), x(1)))
    .toMap
  val batchSize: Int                   = getInt(BATCH_SIZE)
  val batchPrefix: String              = getString(BATCH_PREFIX)
  val batchSeparator: String           = getString(BATCH_SEPARATOR)
  val batchSuffix: String              = getString(BATCH_SUFFIX)
  val maxRetries: Int                  = getInt(MAX_RETRIES)
  val maxBackoff: Long                 = getLong(MAX_BACKOFF_TIMEOUT)
  val maxTimeout: Long                 = getLong(MAX_RETRIES_TIMEOUT)
  val readTimeout: Long                = getLong(READ_TIMEOUT)
  val connectTimeout: Long             = getLong(CONNECT_TIMEOUT)
  val retryBackoff: Long               = getLong(RETRY_BACKOFF)
  val retryBackoffExponential: Boolean = getBoolean(RETRY_EXPONENTIAL_BACKOFF)
  val formatter: String                = getString(FORMATTER)
  val regexPatterns: Seq[String]       = getString(REGEX_PATTERNS).split(regexSep).toList
  val regexReplacements: Seq[String]   = getString(REGEX_REPLACEMENTS).split(regexSep).toList

  val authType: String           = getString(AUTH_TYPE)
  val authHeaderName: String     = getString(AUTH_HEADER_NAME)
  val oauth2TokenUrl: String     = getString(OAUTH2_TOKEN_URL)
  val oauth2ClientId: String     = getString(OAUTH2_CLIENT_ID)
  val oauth2ClientSecret: String = getString(OAUTH2_CLIENT_SECRET)

  val avroConverterConf: Map[String, String] = Map(
    "schema.registry.url"          -> props.get("value.converter.schema.registry.url"),
    "enhanced.avro.schema.support" -> props.get("enhanced.avro.schema.support"),
    "specific.avro.reader"         -> props.get("specific.avro.reader")
  )
}

object HttpSinkConfig {
  val HTTP_API_URL     = "http.api.url"
  val HTTP_API_URL_DOC = "http api url where the data will be sent"

  val HTTP_REQUEST_METHOD         = "http.request.method"
  val HTTP_REQUEST_METHOD_DOC     = "HTTP Request Method"
  val HTTP_REQUEST_METHOD_DEFAULT = "POST"

  val HTTP_HEADERS         = "http.headers"
  val HTTP_HEADERS_DOC     = "http headers to be included in all requests separated by the header.separator"
  val HTTP_HEADERS_DEFAULT = ""

  val HEADERS_SEPARATOR         = "headers.separator"
  val HEADERS_SEPARATOR_DOC     = "separator character used in headers property"
  val HEADERS_SEPARATOR_DEFAULT = "\\|"

  val BATCH_SIZE         = "batch.size"
  val BATCH_SIZE_DOC     = "the number of records accumulated in a batch before the HTTP API will be invoked"
  val BATCH_SIZE_DEFAULT = 1

  val BATCH_PREFIX         = "batch.prefix"
  val BATCH_PREFIX_DEFAULT = ""
  val BATCH_PREFIX_DOC     = "prefix added to record batches that will be added at the beginning of the batch of records"

  val BATCH_SUFFIX         = "batch.suffix"
  val BATCH_SUFFIX_DEFAULT = ""
  val BATCH_SUFFIX_DOC     = "suffix added to record batches that will be applied once at the end of the batch of records"

  val BATCH_SEPARATOR         = "batch.separator"
  val BATCH_SEPARATOR_DEFAULT = ","
  val BATCH_SEPARATOR_DOC     = "separator for records in a batch"

  val READ_TIMEOUT         = "request.read.timeout.ms"
  val READ_TIMEOUT_DOC     = "the maximum request read timeout (-1 = disabled)"
  val READ_TIMEOUT_DEFAULT = 180000

  val CONNECT_TIMEOUT         = "request.connect.timeout.ms"
  val CONNECT_TIMEOUT_DOC     = "the maximum request connect timeout (-1 = disabled)"
  val CONNECT_TIMEOUT_DEFAULT = 30000

  val MAX_RETRIES         = "max.retries"
  val MAX_RETRIES_DOC     = "the maximum number of times to retry on errors before failing the task (-1 = unlimited)"
  val MAX_RETRIES_DEFAULT = 10

  val MAX_RETRIES_TIMEOUT         = "retry.timeout.max.ms"
  val MAX_RETRIES_TIMEOUT_DOC     = "the maximum timeout to retry on errors before failing the task (-1 = disabled)"
  val MAX_RETRIES_TIMEOUT_DEFAULT = 86400000

  val MAX_BACKOFF_TIMEOUT         = "retry.backoff.timeout.ms"
  val MAX_BACKOFF_TIMEOUT_DOC     = "the maximum backoff timeout for a single retry"
  val MAX_BACKOFF_TIMEOUT_DEFAULT = 1800000

  val RETRY_BACKOFF         = "retry.backoff.ms"
  val RETRY_BACKOFF_DOC     = "the duration in milliseconds to wait after an error before a retry attempt is made"
  val RETRY_BACKOFF_DEFAULT = 3000

  val RETRY_EXPONENTIAL_BACKOFF         = "retry.backoff.exponential"
  val RETRY_EXPONENTIAL_BACKOFF_DOC     = "enable exponential backoff mechanism"
  val RETRY_EXPONENTIAL_BACKOFF_DEFAULT = false

  val FORMATTER         = "formatter.type"
  val FORMATTER_DOC     = "Formatter style to use"
  val FORMATTER_DEFAULT = "regex"

  val REGEX_PATTERNS         = "regex.patterns"
  val REGEX_PATTERNS_DOC     = "character separated regex patterns to match for replacement in the destination messages"
  val REGEX_PATTERNS_DEFAULT = ""

  val REGEX_REPLACEMENTS         = "regex.replacements"
  val REGEX_REPLACEMENTS_DOC     = "character separated regex replacements to use with the patterns in regex.patterns"
  val REGEX_REPLACEMENTS_DEFAULT = ""

  val REGEX_SEPARATOR         = "regex.separator"
  val REGEX_SEPARATOR_DOC     = "separator character used in regex.patterns and regex.replacements property."
  val REGEX_SEPARATOR_DEFAULT = "~"

  val AUTH_TYPE         = "auth.type"
  val AUTH_TYPE_DOC     = "HTTP authentication type"
  val AUTH_TYPE_DEFAULT = "none"

  val AUTH_HEADER_NAME         = "auth.header.name"
  val AUTH_HEADER_NAME_DOC     = "Authentication header name"
  val AUTH_HEADER_NAME_DEFAULT = "Authorization"

  val OAUTH2_CLIENT_ID         = "auth.oauth2.client.id"
  val OAUTH2_CLIENT_ID_DOC     = "client-id"
  val OAUTH2_CLIENT_ID_DEFAULT = ""

  val OAUTH2_CLIENT_SECRET         = "auth.oauth2.client.secret"
  val OAUTH2_CLIENT_SECRET_DOC     = "client-secret"
  val OAUTH2_CLIENT_SECRET_DEFAULT = ""

  val OAUTH2_TOKEN_URL         = "auth.oauth2.token.url"
  val OAUTH2_TOKEN_URL_DOC     = "the target endpoint for generating the access token"
  val OAUTH2_TOKEN_URL_DEFAULT = ""

  val DEF: ConfigDef = new ConfigDef()
    .define(HTTP_API_URL, Type.STRING, ConfigDef.NO_DEFAULT_VALUE, Importance.HIGH, HTTP_API_URL_DOC)
    .define(HTTP_REQUEST_METHOD, Type.STRING, HTTP_REQUEST_METHOD_DEFAULT, Importance.HIGH, HTTP_REQUEST_METHOD_DOC)
    .define(HTTP_HEADERS, Type.STRING, HTTP_HEADERS_DEFAULT, Importance.LOW, HTTP_HEADERS_DOC)
    .define(BATCH_SIZE, Type.INT, BATCH_SIZE_DEFAULT, Importance.MEDIUM, BATCH_SIZE_DOC)
    .define(BATCH_PREFIX, Type.STRING, BATCH_PREFIX_DEFAULT, Importance.MEDIUM, BATCH_PREFIX_DOC)
    .define(BATCH_SUFFIX, Type.STRING, BATCH_SUFFIX_DEFAULT, Importance.MEDIUM, BATCH_SUFFIX_DOC)
    .define(BATCH_SEPARATOR, Type.STRING, BATCH_SEPARATOR_DEFAULT, Importance.MEDIUM, BATCH_SEPARATOR_DOC)
    .define(HEADERS_SEPARATOR, Type.STRING, HEADERS_SEPARATOR_DEFAULT, Importance.MEDIUM, HEADERS_SEPARATOR_DOC)
    .define(FORMATTER, Type.STRING, FORMATTER_DEFAULT, Importance.HIGH, FORMATTER_DOC)
    .define(REGEX_PATTERNS, Type.STRING, REGEX_PATTERNS_DEFAULT, Importance.LOW, REGEX_PATTERNS_DOC)
    .define(REGEX_REPLACEMENTS, Type.STRING, REGEX_REPLACEMENTS_DEFAULT, Importance.LOW, REGEX_REPLACEMENTS_DOC)
    .define(REGEX_SEPARATOR, Type.STRING, REGEX_SEPARATOR_DEFAULT, Importance.MEDIUM, REGEX_SEPARATOR_DOC)
    .define(MAX_RETRIES, Type.INT, MAX_RETRIES_DEFAULT, Importance.MEDIUM, MAX_RETRIES_DOC)
    .define(RETRY_BACKOFF, Type.LONG, RETRY_BACKOFF_DEFAULT, Importance.MEDIUM, RETRY_BACKOFF_DOC)
    .define(READ_TIMEOUT, Type.LONG, READ_TIMEOUT_DEFAULT, Importance.MEDIUM, READ_TIMEOUT_DOC)
    .define(CONNECT_TIMEOUT, Type.LONG, CONNECT_TIMEOUT_DEFAULT, Importance.MEDIUM, CONNECT_TIMEOUT_DOC)
    .define(MAX_RETRIES_TIMEOUT, Type.LONG, MAX_RETRIES_TIMEOUT_DEFAULT, Importance.LOW, MAX_RETRIES_TIMEOUT_DOC)
    .define(MAX_BACKOFF_TIMEOUT, Type.LONG, MAX_BACKOFF_TIMEOUT_DEFAULT, Importance.LOW, MAX_BACKOFF_TIMEOUT_DOC)
    .define(RETRY_EXPONENTIAL_BACKOFF, Type.BOOLEAN, RETRY_EXPONENTIAL_BACKOFF_DEFAULT, Importance.LOW, RETRY_EXPONENTIAL_BACKOFF_DOC)
    .define(AUTH_TYPE, Type.STRING, AUTH_TYPE_DEFAULT, Importance.HIGH, AUTH_TYPE_DOC)
    .define(AUTH_HEADER_NAME, Type.STRING, AUTH_HEADER_NAME_DEFAULT, Importance.MEDIUM, AUTH_HEADER_NAME_DOC)
    .define(OAUTH2_CLIENT_ID, Type.STRING, OAUTH2_CLIENT_ID_DEFAULT, Importance.MEDIUM, OAUTH2_CLIENT_ID_DOC)
    .define(
      OAUTH2_CLIENT_SECRET,
      Type.STRING,
      OAUTH2_CLIENT_SECRET_DEFAULT,
      Importance.MEDIUM,
      OAUTH2_CLIENT_SECRET_DOC
    )
    .define(OAUTH2_TOKEN_URL, Type.STRING, OAUTH2_TOKEN_URL_DEFAULT, Importance.MEDIUM, OAUTH2_TOKEN_URL_DOC)

  def apply(props: util.Map[String, String]): HttpSinkConfig = new HttpSinkConfig(props)

  def apply(props: Map[String, String]): HttpSinkConfig = new HttpSinkConfig(props.asJava)
}
