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

import kafka.connect.http.sink.errors.{HttpClientError, MaxAmountOfRetriesReached}
import kafka.connect.http.sink.{HttpSinkConfig, Logging}
import sttp.client3.SttpClientException.ReadException
import sttp.client3._
import sttp.model.{Method, StatusCode}

import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success, Try}

trait Dispatcher extends Logging {
  def send(headers: Map[String, String], body: String, failFast: Boolean = false): Unit
}

final private[dispatcher] class SttpDispatcher(
    private val config: HttpSinkConfig,
    private val backend: SttpBackend[Try, Any],
    private var failedAttempts: Int = 0,
    private var retryUntil: Instant = Instant.MAX
) extends Dispatcher {

  override def send(headers: Map[String, String], body: String, failFast: Boolean): Unit = {
    val response = sendRequest(headers, body)
    if (!response.isSuccess) {
      logger.warn(s"error dispatching data. ${response.code.code}: ${response.statusText}")
      if (failFast) {
        throw MaxAmountOfRetriesReached(response.statusText)
      } else {
        retry(response, headers, body)
      }
    } else {
      // reset failed attempts
      retryUntil = Instant.MAX
      failedAttempts = 0
    }
  }

  private def retry(response: Response[Either[String, String]], headers: Map[String, String], body: String): Unit = {
    failedAttempts += 1
    if (retryUntil != Instant.MAX) {
      retryUntil = Instant.now().plusMillis(config.maxTimeout)
    }
    if (
      (failedAttempts <= config.maxRetries || config.maxRetries == -1)
      && retryUntil.isAfter(Instant.now())
    ) {
      if (config.retryBackoffExponential) {
        Thread.sleep(math.min(config.maxBackoff, (config.retryBackoff * math.pow(2, (failedAttempts - 1).toDouble)).longValue))
      } else {
        Thread.sleep(config.retryBackoff)
      }
      send(headers, body)
    } else {
      throw MaxAmountOfRetriesReached(response.statusText)
    }
  }

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
        throw HttpClientError(f.getMessage)
    }
}

object Dispatcher {
  def sttp(config: HttpSinkConfig, backend: SttpBackend[Try, Any]): Dispatcher =
    new SttpDispatcher(config, backend)
}
