package io.kirill.kafka.connect.http.sink

import java.time.Instant

import io.kirill.kafka.connect.http.sink.authenticator.Authenticator
import io.kirill.kafka.connect.http.sink.dispatcher.Dispatcher
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HttpWriterSpec extends AnyWordSpec with Matchers with MockitoSugar {

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

    "add records into a batch" in {
      val config = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "3",
        "http.headers" -> "content-type:application/json|accept:application/json"
      ))
      val records = List(record(), record())
      val (authenticator, dispatcher) = mocks
      val writer = new HttpWriter(config, dispatcher, Some(authenticator))

      writer.put(records)

      writer.currentBatch must be (records)

      verifyZeroInteractions(dispatcher, authenticator)
    }

    "send records when batch is full until it is empty" in {
      val config = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "1",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json|accept:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val (authenticator, dispatcher) = mocks
      when(authenticator.authHeader()).thenReturn("Basic access-token")
      val writer = new HttpWriter(config, dispatcher, Some(authenticator))

      writer.put(List(record("r1"), record("r2"), record("r3")))

      writer.currentBatch must be (Nil)

      verify(authenticator, times(3)).authHeader()
      val expectedHeaders = Map("content-type" -> "application/json", "accept" -> "application/json", "Authorization" -> "Basic access-token")
      verify(dispatcher).send(expectedHeaders, """[{"name":"r1","age":"21"}]""")
      verify(dispatcher).send(expectedHeaders, """[{"name":"r2","age":"21"}]""")
      verify(dispatcher).send(expectedHeaders, """[{"name":"r3","age":"21"}]""")
    }

    "send records when timer is out" in {
      val config = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "5",
        "batch.interval.ms" -> "100",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val (_, dispatcher) = mocks
      val writer = new HttpWriter(config, dispatcher, None)

      writer.time = Instant.now().toEpochMilli - 1000
      writer.put(List(record("r1"), record("r2"), record("r3")))

      writer.currentBatch must be (Nil)

      verify(dispatcher).send(Map("content-type" -> "application/json"), "[{\"name\":\"r1\",\"age\":\"21\"},{\"name\":\"r2\",\"age\":\"21\"},{\"name\":\"r3\",\"age\":\"21\"}]")
    }

    "flush records" in {
      val config = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:12345/events",
        "batch.size" -> "1",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "http.headers" -> "content-type:application/json",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
      ))

      val (_, dispatcher) = mocks
      val writer = new HttpWriter(config, dispatcher, None)

      writer.currentBatch = List(record("r1"))
      writer.flush

      writer.currentBatch must be (Nil)

      verify(dispatcher).send(Map("content-type" -> "application/json"), """[{"name":"r1","age":"21"}]""")
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

  def mocks: (Authenticator, Dispatcher) =
    (mock[Authenticator], mock[Dispatcher])
}
