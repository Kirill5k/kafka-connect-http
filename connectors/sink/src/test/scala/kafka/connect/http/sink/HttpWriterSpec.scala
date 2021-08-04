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

package kafka.connect.http.sink

import kafka.connect.http.sink.authenticator.Authenticator
import kafka.connect.http.sink.dispatcher.Dispatcher
import kafka.connect.http.sink.errors.HttpClientError
import kafka.connect.http.sink.formatter.Formatter
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.{SinkRecord, SinkTaskContext}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.DurationInt

class HttpWriterSpec extends AnyWordSpec with Matchers with MockitoSugar {

  implicit val ec = scala.concurrent.ExecutionContext.global
  val records = List(
    new SinkRecord("topic", 0, null, null, null, null, 0),
    new SinkRecord("topic", 0, null, null, null, null, 0),
    new SinkRecord("topic", 0, null, null, null, null, 0)
  )
  val json = """[{"name":"John Smith","age":"21"},{"name":"John Bloggs","age":"21"}]"""
  val tp   = new TopicPartition("topic", 0)

  "A HttpWriter" should {

    "add records into a batch" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url" -> "http://localhost:12345/events",
          "batch.size"   -> "5"
        )
      )

      val (_, dispatcher, formatter, ctx) = mocks
      val writer                          = new HttpWriter(config, dispatcher, formatter, None, Some(ctx))
      when(dispatcher.send(any[Map[String, String]], any[String], any[Boolean])).thenReturn(None)
      when(formatter.toOutputFormat(any[List[SinkRecord]])).thenReturn("")
      writer.put(records)
      verify(dispatcher, times(1)).send(
        Map(),
        "",
        true
      )
      writer.put(records)
      writer.batches must contain theSameElementsAs records
    }

    "send records when batch is full until it is empty" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url" -> "http://localhost:12345/events",
          "batch.size"   -> "1",
          "http.headers" -> "content-type:application/json|accept:application/json"
        )
      )

      val (authenticator, dispatcher, formatter, ctx) = mocks

      when(dispatcher.send(any[Map[String, String]], any[String], any[Boolean])).thenReturn(None)
      when(formatter.toOutputFormat(any[List[SinkRecord]])).thenReturn(json)
      when(authenticator.authHeader()).thenReturn("Basic access-token")
      val writer = new HttpWriter(config, dispatcher, formatter, Some(authenticator), Some(ctx))

      writer.put(records)

      writer.batches.size must be(records.size - 1)
      writer.flush()
      writer.batches must be(Nil)

      verify(authenticator, times(3)).authHeader()
      verify(dispatcher, times(1)).send(
        Map(
          "content-type"  -> "application/json",
          "accept"        -> "application/json",
          "Authorization" -> "Basic access-token"
        ),
        json,
        true
      )
      verify(dispatcher, times(2)).send(
        Map(
          "content-type"  -> "application/json",
          "accept"        -> "application/json",
          "Authorization" -> "Basic access-token"
        ),
        json,
        false
      )
    }

    "flush records" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url" -> "http://localhost:12345/events",
          "batch.size"   -> "5",
          "http.headers" -> "content-type:application/json"
        )
      )

      val (_, dispatcher, formatter, ctx) = mocks
      when(formatter.toOutputFormat(any[List[SinkRecord]])).thenReturn(json)
      when(dispatcher.send(any[Map[String, String]], any[String], any[Boolean])).thenReturn(None)
      val writer = new HttpWriter(config, dispatcher, formatter, None, Some(ctx))

      writer.batches = records
      writer.flush()

      writer.batches must be(Nil)

      verify(formatter).toOutputFormat(records)
      verify(dispatcher).send(Map("content-type" -> "application/json"), json, true)
    }

    "pause after error" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url"     -> "http://localhost:12345/events",
          "batch.size"       -> "5",
          "http.headers"     -> "content-type:application/json",
          "retry.backoff.ms" -> "100"
        )
      )

      val (_, dispatcher, formatter, ctx) = mocks
      when(formatter.toOutputFormat(any[List[SinkRecord]])).thenReturn(json)
      when(dispatcher.send(any[Map[String, String]], any[String], any[Boolean])).thenReturn(Some(HttpClientError("")))
      when(dispatcher.pauseTime).thenReturn(100.milliseconds)
      val writer = new HttpWriter(config, dispatcher, formatter, None, Some(ctx))

      writer.batches = records
      writer.flush()

      writer.batches must be(Nil)

      verify(dispatcher).pauseTime
      verify(ctx).pause(tp, tp, tp)
      verify(ctx).timeout(100L)
      verify(formatter).toOutputFormat(records)
      verify(dispatcher).send(Map("content-type" -> "application/json"), json, true)
      Thread.sleep(1000L)
      verify(ctx).resume(tp, tp, tp)
    }
  }

  def mocks: (Authenticator, Dispatcher, Formatter, SinkTaskContext) =
    (mock[Authenticator], mock[Dispatcher], mock[Formatter], mock[SinkTaskContext])
}
