package io.kirill.kafka.connect.http.sink

import java.time.Instant

import io.kirill.kafka.connect.http.sink.authenticator.Authenticator
import io.kirill.kafka.connect.http.sink.dispatcher.Dispatcher
import io.kirill.kafka.connect.http.sink.formatter.Formatter
import org.apache.kafka.connect.sink.SinkRecord
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HttpWriterSpec extends AnyWordSpec with Matchers with MockitoSugar {

  val records = List(
    new SinkRecord("topic", 0, null, null, null, null, 0),
    new SinkRecord("topic", 0, null, null, null, null, 0),
    new SinkRecord("topic", 0, null, null, null, null, 0)
  )
  val json = """[{"name":"John Smith","age":"21"},{"name":"John Bloggs","age":"21"}]"""

  "A HttpWriter" should {

    "add records into a batch" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url" -> "http://localhost:12345/events",
          "batch.size"   -> "5"
        )
      )

      val (authenticator, dispatcher, formatter) = mocks
      val writer                                 = new HttpWriter(config, dispatcher, formatter, Some(authenticator))

      writer.put(records)

      writer.currentBatch must be(records)

      verifyZeroInteractions(dispatcher, authenticator, formatter)
    }

    "send records when batch is full until it is empty" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url" -> "http://localhost:12345/events",
          "batch.size"   -> "1",
          "http.headers" -> "content-type:application/json|accept:application/json"
        )
      )

      val (authenticator, dispatcher, formatter) = mocks

      when(formatter.toJson(any[List[SinkRecord]])).thenReturn(json)
      when(authenticator.authHeader()).thenReturn("Basic access-token")
      val writer = new HttpWriter(config, dispatcher, formatter, Some(authenticator))

      writer.put(records)

      writer.currentBatch must be(Nil)

      verify(authenticator, times(3)).authHeader()
      verify(dispatcher, times(3)).send(
        Map(
          "content-type"  -> "application/json",
          "accept"        -> "application/json",
          "Authorization" -> "Basic access-token"
        ),
        json
      )
    }

    "send records when timer is out" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url"      -> "http://localhost:12345/events",
          "batch.size"        -> "5",
          "batch.interval.ms" -> "100",
          "http.headers"      -> "content-type:application/json"
        )
      )

      val (_, dispatcher, formatter) = mocks
      when(formatter.toJson(any[List[SinkRecord]])).thenReturn(json)
      val writer = new HttpWriter(config, dispatcher, formatter, None)

      writer.time = Instant.now().toEpochMilli - 1000
      writer.put(records)

      writer.currentBatch must be(Nil)

      verify(formatter).toJson(records)
      verify(dispatcher).send(Map("content-type" -> "application/json"), json)
    }

    "flush records" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url" -> "http://localhost:12345/events",
          "batch.size"   -> "5",
          "http.headers" -> "content-type:application/json"
        )
      )

      val (_, dispatcher, formatter) = mocks
      when(formatter.toJson(any[List[SinkRecord]])).thenReturn(json)
      val writer = new HttpWriter(config, dispatcher, formatter, None)

      writer.currentBatch = records
      writer.flush()

      writer.currentBatch must be(Nil)

      verify(formatter).toJson(records)
      verify(dispatcher).send(Map("content-type" -> "application/json"), json)
    }
  }

  def mocks: (Authenticator, Dispatcher, Formatter) =
    (mock[Authenticator], mock[Dispatcher], mock[Formatter])
}
