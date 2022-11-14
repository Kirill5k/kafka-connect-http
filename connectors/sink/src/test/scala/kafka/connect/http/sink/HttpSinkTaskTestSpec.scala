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

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.SinkRecord
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class HttpSinkTaskTestSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "A HttpSinkTask" should {

    "start" in {
      val task  = new HttpSinkTask()
      val props = Map("http.api.url" -> "http://foo.bar").asJava

      task.start(props)

      task.writer.get mustBe a[HttpWriter]
    }

    "put" in {
      val writerMock = mock[HttpWriter]
      val task       = new HttpSinkTask()
      task.writer = Some(writerMock)

      val records = List(new SinkRecord("topic", 0, null, null, null, "value", 0))
      task.put(records.asJava)

      verify(writerMock).put(eqTo(records))
    }

    "put when null value" in {
      val writerMock = mock[HttpWriter]
      val task       = new HttpSinkTask()
      task.writer = Some(writerMock)

      val records = List(new SinkRecord("topic", 0, null, null, null, null, 0))
      task.put(records.asJava)

      verify(writerMock, never).put(any[List[SinkRecord]])
    }

    "flush" in {
      val writerMock = mock[HttpWriter]
      val task       = new HttpSinkTask()
      task.writer = Some(writerMock)

      when(writerMock.flush()).thenReturn(Map.empty[TopicPartition, OffsetAndMetadata])
      task.flush(Map[TopicPartition, OffsetAndMetadata]().asJava)

      verify(writerMock).flush()
    }
  }
}
