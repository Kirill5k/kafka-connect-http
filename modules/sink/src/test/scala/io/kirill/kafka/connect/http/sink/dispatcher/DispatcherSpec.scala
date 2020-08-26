package io.kirill.kafka.connect.http.sink.dispatcher

import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpError
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.verify.VerificationTimes
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client.HttpURLConnectionBackend

import scala.concurrent.ExecutionContext

class DispatcherSpec extends AnyWordSpec with Matchers {
  implicit val ex: ExecutionContext = scala.concurrent.ExecutionContext.global

  val backend = HttpURLConnectionBackend()

  val config = HttpSinkConfig(
    Map(
      "http.api.url" -> "http://localhost:12345/data",
      "max.retries" -> "4",
      "retry.backoff.ms" -> "0"
    )
  )

  "A LiveDispatcher" should {

    "send http request" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/data"))
        .respond(response().withStatusCode(200))

      val dispatcher = Dispatcher.sttp(config, backend)

      dispatcher.send(Map("Content-Type" -> "application/x-www-form-urlencoded"), "foo=bar")

      server.verify(
        request()
          .withHeader("Content-Type", "application/x-www-form-urlencoded")
          .withMethod("POST")
          .withBody("foo=bar")
      )
    }

    "retry on failed attempt" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/data"), Times.exactly(2))
        .respond(response().withStatusCode(500).withBody("error"))
      server
        .when(request().withMethod("POST").withPath("/data"))
        .respond(response().withStatusCode(200))

      val dispatcher = Dispatcher.sttp(config, backend)

      dispatcher.send(Map("Content-Type" -> "application/x-www-form-urlencoded"), "foo=bar")

      server.verify(
        request()
          .withHeader("Content-Type", "application/x-www-form-urlencoded")
          .withMethod("POST")
          .withBody("foo=bar"),
        VerificationTimes.exactly(3)
      )
    }
  }

  def withMockserver(test: ClientAndServer => Any): Any = {
    val server = new ClientAndServer(12345)
    try {
      test(server)
    } finally {
      server.stop()
    }
  }
}
