package io.kirill.kafka.connect.http.sink

import java.time.Instant

import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.verify.VerificationTimes
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.concurrent.ExecutionContext

class HttpWriterSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {
  implicit val ex: ExecutionContext = scala.concurrent.ExecutionContext.global

  "A HttpWriter" should {

    "format sink records" in {
      val props = Map(
        "http.api.url" -> "http://localhost:12345/events",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "headers.separator" -> "\\|",
        "batch.size" -> "100",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "max.retries" -> "3",
        "retry.backoff.ms" -> "10",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      )

      val conf = HttpSinkConfig(props)
      val records = List(record("John Smith"), record("John Bloggs"))

      val formattedRecords = HttpWriter.formatRecords(conf, records)

      formattedRecords must be("""[{"name":"John Smith","age":"21"},{"name":"John Bloggs","age":"21"}]""")
    }

    "send http request" in withMockserver { server =>
      server
        .when(request().withMethod("PUT").withPath("/events"))
        .respond(response().withStatusCode(200))

      val conf = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "http.request.method" -> "PUT",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "headers.separator" -> "\\|"
      ))

      val res = HttpWriter.sendRequest(conf, "{\"foo\": \"bar\"}")

      res.code must be (200)

      server.verify(
        request()
          .withHeader("Content-Type", "application/json")
          .withHeader("Accept", "application/json")
          .withMethod("PUT")
          .withBody("{\"foo\": \"bar\"}")
      )
    }

    "add records into a batch" in withMockserver { server =>
      val conf = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "3",
        "http.headers" -> "content-type:application/json|accept:application/json"
      ))
      val records = List(record(), record())
      val writer = HttpWriter(conf)

      writer.put(records)

      writer.currentBatch must be (records)

      server.verifyZeroInteractions()
    }

    "send records when batch is full until it is empty" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/events"))
        .respond(response().withStatusCode(200))

      val conf = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "1",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val writer = HttpWriter(conf)

      writer.put(List(record("r1"), record("r1"), record("r1")))

      Thread.sleep(1000)

      writer.currentBatch must be (Nil)

      server.verify(
        request().withBody("[{\"name\":\"r1\",\"age\":\"21\"}]"), VerificationTimes.exactly(3)
      )
    }

    "send records when timer is out" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/events"))
        .respond(response().withStatusCode(200))

      val conf = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "5",
        "batch.interval.ms" -> "100",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val writer = HttpWriter(conf)
      writer.time = Instant.now().toEpochMilli - 1000

      writer.put(List(record("r1"), record("r2"), record("r3")))

      Thread.sleep(1000)

      writer.currentBatch must be (Nil)

      server.verify(
        request().withBody("[{\"name\":\"r1\",\"age\":\"21\"},{\"name\":\"r2\",\"age\":\"21\"},{\"name\":\"r3\",\"age\":\"21\"}]")
      )
    }

    "flush records" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/events"))
        .respond(response().withStatusCode(200))

      val conf = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "1",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val writer = HttpWriter(conf)
      writer.currentBatch = List(record("r1"))

      writer.flush

      Thread.sleep(1000)

      writer.currentBatch must be (Nil)

      server.verify(
        request().withBody("[{\"name\":\"r1\",\"age\":\"21\"}]")
      )
    }

    "retry on error" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/events"), Times.exactly(2))
        .respond(response().withStatusCode(500).withBody("error"))
      server
        .when(request().withMethod("POST").withPath("/events"))
        .respond(response().withBody("ok"))

      val conf = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "1",
        "retry.backoff.ms" -> "100",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val writer = HttpWriter(conf)

      writer.put(List(record("r1")))

      Thread.sleep(1000)

      writer.currentBatch must be (Nil)
      writer.failedAttempts must be (2)

      server.verify(
        request().withBody("[{\"name\":\"r1\",\"age\":\"21\"}]"), VerificationTimes.exactly(3)
      )
    }

    "throw an exception when max amount of retries reached" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/events"), Times.exactly(2))
        .respond(response().withStatusCode(500).withBody("error"))

      val conf = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "1",
        "max.retries" -> "1",
        "retry.backoff.ms" -> "100",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val writer = HttpWriter(conf)

      writer.put(List(record("r1")))
      Thread.sleep(1000)

      writer.failedAttempts must be(2)

      server.verify(
        request().withBody("[{\"name\":\"r1\",\"age\":\"21\"}]"), VerificationTimes.exactly(2)
      )
    }
  }

  def record(name: String = "John Bloggs"): SinkRecord = {
    val schema = SchemaBuilder
      .struct()
      .name("com.example.Person")
      .field("name", Schema.STRING_SCHEMA)
      .field("age", Schema.INT32_SCHEMA)
      .build()
    val message = new Struct(schema)
      .put("name", name)
      .put("age", 21)
    new SinkRecord("topic", 1, null, "key", schema, message, 1)
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
