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
import kafka.connect.http.sink.formatter.Formatter
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

      writer.atLeastOneSent = true
      writer.put(records)

      writer.batches must contain theSameElementsAs records

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

      when(formatter.toOutputFormat(any[List[SinkRecord]])).thenReturn(json)
      when(authenticator.authHeader()).thenReturn("Basic access-token")
      val writer = new HttpWriter(config, dispatcher, formatter, Some(authenticator))

      writer.put(records)

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

      val (_, dispatcher, formatter) = mocks
      when(formatter.toOutputFormat(any[List[SinkRecord]])).thenReturn(json)
      val writer = new HttpWriter(config, dispatcher, formatter, None)

      writer.batches = records
      writer.flush()

      writer.batches must be(Nil)

      verify(formatter).toOutputFormat(records)
      verify(dispatcher).send(Map("content-type" -> "application/json"), json, true)
    }
  }

  def mocks: (Authenticator, Dispatcher, Formatter) =
    (mock[Authenticator], mock[Dispatcher], mock[Formatter])
}
