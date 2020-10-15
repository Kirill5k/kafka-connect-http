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

package io.kirill.kafka.connect.http.sink

import java.time.Instant

import io.kirill.kafka.connect.http.sink.authenticator.Authenticator
import io.kirill.kafka.connect.http.sink.dispatcher.Dispatcher
import io.kirill.kafka.connect.http.sink.formatter.Formatter
import org.apache.kafka.connect.sink.SinkRecord
import sttp.client.TryHttpURLConnectionBackend

class HttpWriter(
    private val config: HttpSinkConfig,
    private val dispatcher: Dispatcher,
    private val formatter: Formatter,
    private val authenticator: Option[Authenticator]
) extends Logging {

  var currentBatch: List[SinkRecord] = List()
  var time: Long                     = Instant.now.toEpochMilli

  def put(records: List[SinkRecord]): Unit = {
    val currentTime = Instant.now().toEpochMilli
    if (currentBatch.size + records.size >= config.batchSize || currentTime - time >= config.batchIntervalMs) {
      time = currentTime
      val (batch, remaining) = (currentBatch ++ records).splitAt(config.batchSize)
      sendBatch(batch)
      if (remaining.nonEmpty) {
        put(remaining)
      }
    } else {
      currentBatch = currentBatch ++ records
    }
  }

  def flush(): Unit = {
    sendBatch(currentBatch)
    currentBatch = List()
  }

  private def sendBatch(records: List[SinkRecord]): Unit = {
    val body    = formatter.toJson(records)
    val headers = authenticator.fold(config.httpHeaders)(a => config.httpHeaders + ("Authorization" -> a.authHeader()))
    dispatcher.send(headers, body)
  }
}

object HttpWriter {

  def make(config: HttpSinkConfig): HttpWriter = {
    val backend    = TryHttpURLConnectionBackend()
    val dispatcher = Dispatcher.sttp(config, backend)
    val formatter  = Formatter.regexBased(config)
    val authenticator = config.authType match {
      case "oauth2" => Some(Authenticator.oauth2(config, backend))
      case _        => None
    }
    new HttpWriter(config, dispatcher, formatter, authenticator)
  }
}
