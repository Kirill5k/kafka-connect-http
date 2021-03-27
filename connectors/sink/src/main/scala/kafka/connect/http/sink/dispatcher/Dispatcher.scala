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

import kafka.connect.http.sink.errors.{HttpClientError, MaxAmountOfRetriesReached}
import kafka.connect.http.sink.{HttpSinkConfig, Logging}
import sttp.client3._
import sttp.model.Method

import scala.util.{Failure, Success, Try}

trait Dispatcher extends Logging {
  def send(headers: Map[String, String], body: String): Unit
}

final private[dispatcher] class SttpDispatcher(
    private val config: HttpSinkConfig,
    private val backend: SttpBackend[Try, Any],
    private var failedAttempts: Int = 0
) extends Dispatcher {

  override def send(headers: Map[String, String], body: String): Unit = {
    val response = sendRequest(headers, body)
    if (!response.isSuccess) {
      logger.error(s"error dispatching data. ${response.code.code}: ${response.body.fold(s => s, s => s)}")
      retry(headers, body)
    }
  }

  private def retry(headers: Map[String, String], body: String): Unit = {
    failedAttempts += 1
    if (failedAttempts <= config.maxRetries) {
      Thread.sleep(config.retryBackoff)
      send(headers, body)
    } else {
      throw MaxAmountOfRetriesReached
    }
  }

  private def sendRequest(headers: Map[String, String], body: String): Response[Either[String, String]] =
    backend.send(
      basicRequest
        .headers(headers)
        .body(body)
        .method(Method(config.httpRequestMethod), uri"${config.httpApiUrl}")
    ) match {
      case Success(value)     => value
      case Failure(exception) => throw HttpClientError(exception.getMessage)
    }
}

object Dispatcher {
  def sttp(config: HttpSinkConfig, backend: SttpBackend[Try, Any]): Dispatcher =
    new SttpDispatcher(config, backend)
}
