package io.kirill.kafka.connect.http.sink.dispatcher

import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import io.kirill.kafka.connect.http.sink.errors.SinkError
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client.testing.SttpBackendStub
import sttp.client.{Response, StringBody}
import sttp.model.{Header, Method, StatusCode}

class DispatcherSpec extends AnyWordSpec with Matchers {

  val config = HttpSinkConfig(
    Map(
      "http.api.url" -> "http://localhost:8080/data",
      "http.request.method" -> "PUT",
      "max.retries" -> "4",
      "retry.backoff.ms" -> "0"
    )
  )

  "A LiveDispatcher" should {

    "send http request" in {
      val backend = SttpBackendStub.synchronous
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
      val backend = SttpBackendStub.synchronous
        .whenAnyRequest
        .thenRespondCyclicResponses(
          Response("error", StatusCode.InternalServerError, "Something went wrong"),
          Response("error", StatusCode.InternalServerError, "Something went wrong"),
          Response.ok("ok")
        )

      val dispatcher = Dispatcher.sttp(config, backend)

      dispatcher.send(Map("Content-Type" -> "application/json"), "{\"foo\":\"bar\"}")
    }

    "thrown an exception when number of retries is greater than max" in {
      val backend = SttpBackendStub.synchronous
        .whenAnyRequest
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

      error.message must be ("reached the maximum number of times to retry on errors before failing the task")
    }
  }
}
