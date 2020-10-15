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

object errors {

  sealed trait SinkError extends Throwable {
    val message: String
    override def getMessage: String = message
  }

  final case class HttpClientError(message: String) extends SinkError

  final case class AuthError(message: String) extends SinkError

  final case class JsonParsingError(json: String) extends SinkError {
    val message = s"error parsing json from a response: ${json}"
  }

  final case object MaxAmountOfRetriesReached extends SinkError {
    val message: String = "reached the maximum number of times to retry on errors before failing the task"
  }
}
