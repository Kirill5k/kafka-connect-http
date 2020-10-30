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

import kafka.connect.http.sink.errors.SinkError
import kafka.connect.http.sink.HttpSinkConfig
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client.monad.TryMonad
import sttp.client.testing.SttpBackendStub
import sttp.client.{NothingT, Response, StringBody}
import sttp.model.{Header, Method, StatusCode}

import scala.util.Try

class DispatcherSpec extends AnyWordSpec with Matchers {

  val config = HttpSinkConfig(
    Map(
      "http.api.url"        -> "http://localhost:8080/data",
      "http.request.method" -> "PUT",
      "max.retries"         -> "4",
      "retry.backoff.ms"    -> "0"
    )
  )

  "A LiveDispatcher" should {

    "send http request" in {
      val backend = SttpBackendStub[Try, Nothing, NothingT](TryMonad)
        .whenRequestMatches { r =>
          r.method == Method.PUT &&
          r.headers.contains(Header("Content-Type", "application/json"))
          r.body.asInstanceOf[StringBody].s == "{\"foo\":\"bar\"}"
        }
        .thenRespond(Response.ok("ok"))

      val dispatcher = Dispatcher.sttp(config, backend)

      dispatcher.send(Map("Content-Type" -> "application/json"), "{\"foo\":\"bar\"}")
    }

    "retry on failed attempt" in {
      val backend = SttpBackendStub[Try, Nothing, NothingT](TryMonad).whenAnyRequest
        .thenRespondCyclicResponses(
          Response("error", StatusCode.InternalServerError, "Something went wrong"),
          Response("error", StatusCode.InternalServerError, "Something went wrong"),
          Response.ok("ok")
        )

      val dispatcher = Dispatcher.sttp(config, backend)

      dispatcher.send(Map("Content-Type" -> "application/json"), "{\"foo\":\"bar\"}")
    }

    "thrown an exception when number of retries is greater than max" in {
      val backend = SttpBackendStub[Try, Nothing, NothingT](TryMonad).whenAnyRequest
        .thenRespondCyclicResponses(
          Response("error", StatusCode.InternalServerError, "Something went wrong"),
          Response("error", StatusCode.InternalServerError, "Something went wrong"),
          Response("error", StatusCode.InternalServerError, "Something went wrong"),
          Response("error", StatusCode.InternalServerError, "Something went wrong")
        )

      val dispatcher = Dispatcher.sttp(config, backend)

      val error = intercept[SinkError] {
        dispatcher.send(Map("Content-Type" -> "application/json"), "{\"foo\":\"bar\"}")
      }

      error.message must be("reached the maximum number of times to retry on errors before failing the task")
    }
  }
}
