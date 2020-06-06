package io.kirill.kafka.connect.http.sink

import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.concurrent.ExecutionContext

class HttpWriterSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {
  implicit val ex: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockServer = new MockWebServer()
  val apiUrl = mockServer.url("/api/")

  override def afterAll(): Unit = {
    mockServer.shutdown()
  }

  "A HttpWriter" should {

    "format sink records" in {
      val props = java.util.Map.of(
        "http.api.url", apiUrl.toString,
        "http.headers", "content-type:application/json|accept:application/json",
        "headers.separator", "\\|",
        "batch.size", "100",
        "batch.prefix", "[",
        "batch.suffix", "]",
        "max.retries", "3",
        "retry.backoff.ms", "10",
        "regex.patterns", ",~=~Struct\\{~\\}",
        "regex.replacements", "\",\"~\":\"~{\"~\"}",
      )

      val conf = HttpSinkConfig(props)
      val records = List(record("John Smith"), record("John Bloggs"))

      val formattedRecords = HttpWriter.formatRecords(conf, records)

      formattedRecords must be("""[{"name":"John Smith","age":"21"},{"name":"John Bloggs","age":"21"}]""")
    }

    "send http request" in {
      mockServer.enqueue(new MockResponse().setResponseCode(200))

      val conf = HttpSinkConfig(java.util.Map.of(
        "http.api.url", apiUrl.toString,
        "http.request.method", "PUT",
        "http.headers", "content-type:application/json|accept:application/json",
        "headers.separator", "\\|",
      ))

      val response = HttpWriter.sendRequest(conf, "{\"foo\": \"bar\"}")

      response.code must be (200)

      val recordedRequest = mockServer.takeRequest()
      recordedRequest.getHeader("content-type") must be ("application/json")
      recordedRequest.getHeader("accept") must be ("application/json")
      recordedRequest.getMethod must be ("PUT")
    }

    "add records into a batch" in {
      val conf = HttpSinkConfig(java.util.Map.of(
        "http.api.url", apiUrl.toString,
        "batch.size", "3",
        "http.headers", "content-type:application/json|accept:application/json"
      ))
      val records = List(record(), record())
      val writer = HttpWriter(conf)

      writer.put(records)

      writer.currentBatch must be (records)
    }

    "send records when batch is full until it is empty" in {
      mockServer.enqueue(new MockResponse().setResponseCode(200))
      mockServer.enqueue(new MockResponse().setResponseCode(200))
      mockServer.enqueue(new MockResponse().setResponseCode(200))

      val conf = HttpSinkConfig(java.util.Map.of(
        "http.api.url", apiUrl.toString,
        "batch.size", "1",
        "http.headers", "content-type:application/json|accept:application/json"
      ))

      val writer = HttpWriter(conf)

      writer.put(List(record(), record(), record()))

      Thread.sleep(1000)

      writer.currentBatch must be (Nil)
    }

    "flush records" in {
      mockServer.enqueue(new MockResponse().setResponseCode(200))

      val conf = HttpSinkConfig(java.util.Map.of(
        "http.api.url", apiUrl.toString,
        "batch.size", "1",
        "http.headers", "content-type:application/json|accept:application/json"
      ))

      val writer = HttpWriter(conf)
      writer.currentBatch = List(record())

      writer.flush

      Thread.sleep(1000)

      writer.currentBatch must be (Nil)
    }

    "retry on error" in {
      mockServer.enqueue(new MockResponse().setResponseCode(400))
      mockServer.enqueue(new MockResponse().setResponseCode(400))
      mockServer.enqueue(new MockResponse().setResponseCode(200))

      val conf = HttpSinkConfig(java.util.Map.of(
        "http.api.url", apiUrl.toString,
        "batch.size", "1",
        "retry.backoff.ms", "100",
        "http.headers", "content-type:application/json|accept:application/json"
      ))

      val writer = HttpWriter(conf)

      writer.put(List(record()))

      Thread.sleep(1000)

      writer.currentBatch must be (Nil)
      writer.failedAttempts must be (2)
    }

    "throw an exception when max amount of retries reached" in {
      mockServer.enqueue(new MockResponse().setResponseCode(400))
      mockServer.enqueue(new MockResponse().setResponseCode(400))

      val conf = HttpSinkConfig(java.util.Map.of(
        "http.api.url", apiUrl.toString,
        "batch.size", "1",
        "max.retries", "1",
        "retry.backoff.ms", "100",
        "http.headers", "content-type:application/json|accept:application/json"
      ))

      val writer = HttpWriter(conf)

      writer.put(List(record()))
      Thread.sleep(1000)

      writer.failedAttempts must be(2)
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
}
