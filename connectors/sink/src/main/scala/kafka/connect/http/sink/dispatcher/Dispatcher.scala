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

package kafka.connect.http.sink.dispatcher

import java.net.ConnectException
import java.time.Instant

import kafka.connect.http.sink.errors.{HttpClientError, MaxAmountOfRetriesReached, SinkError}
import kafka.connect.http.sink.{HttpSinkConfig, Logging}
import sttp.client3.SttpClientException.ReadException
import sttp.client3._
import sttp.model.{Method, StatusCode}

import scala.concurrent.duration.{Duration, DurationLong}
import scala.util.{Failure, Success, Try}

trait Dispatcher extends Logging {
  def send(headers: Map[String, String], body: String, failFast: Boolean = false): Option[SinkError]
  def pauseTime: Duration
}

final private[dispatcher] class SttpDispatcher(
    private val config: HttpSinkConfig,
    private val backend: SttpBackend[Try, Any],
    private var failedAttempts: Int = 0,
    private var retryUntil: Instant = Instant.MAX
) extends Dispatcher {

  override def send(headers: Map[String, String], body: String, failFast: Boolean): Option[SinkError] = {
    val response = sendRequest(headers, body)
    if (!response.isSuccess) {
      logger.info(s"error dispatching data URL: ${config.httpApiUrl}, ${response.show(includeBody = true).take(1024)}")
      if (shouldRetry(failFast)) {
        failedAttempts += 1
        if (retryUntil == Instant.MAX) {
          retryUntil = Instant.now().plusMillis(config.maxTimeout)
        }
        Some(HttpClientError(response.statusText))
      } else {
        Some(MaxAmountOfRetriesReached(response.statusText))
      }
    } else {
      // reset failed attempts
      retryUntil = Instant.MAX
      failedAttempts = 0
      Option.empty
    }
  }

  override def pauseTime: Duration =
    if (config.retryBackoffExponential) {
      math.min(config.maxBackoff, (config.retryBackoff * math.pow(2, (failedAttempts - 1).toDouble)).longValue).millisecond
    } else {
      config.retryBackoff.millisecond
    }

  private def shouldRetry(failFast: Boolean): Boolean =
    !failFast && (failedAttempts <= config.maxRetries || config.maxRetries == -1) && retryUntil.isAfter(Instant.now())

  private def sendRequest(headers: Map[String, String], body: String): Response[Either[String, String]] =
    backend.send(
      basicRequest
        .headers(headers)
        .readTimeout(config.readTimeout.milliseconds)
        .body(body)
        .method(Method(config.httpRequestMethod), uri"${config.httpApiUrl}")
    ) match {
      case Success(value)                    => value
      case Failure(exception: ReadException) =>
        // Create virtual response
        Response.apply(Left(exception.getCause.getMessage), StatusCode(-1), exception.getCause.getMessage)
      case Failure(exception: ConnectException) =>
        // Create virtual response
        Response.apply(Left(exception.getCause.getMessage), StatusCode(-1), exception.getCause.getMessage)
      case Failure(f) =>
        Response.apply(Left(f), StatusCode(-1), f.getMessage)
    }
}

object Dispatcher {
  def sttp(config: HttpSinkConfig, backend: SttpBackend[Try, Any]): Dispatcher =
    new SttpDispatcher(config, backend)
}
