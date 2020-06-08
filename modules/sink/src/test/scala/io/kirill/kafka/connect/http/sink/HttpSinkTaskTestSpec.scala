package io.kirill.kafka.connect.http.sink

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.SinkRecord
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

class HttpSinkTaskTestSpec extends AnyWordSpec with Matchers with MockitoSugar {
  implicit val ec = scala.concurrent.ExecutionContext.global

  "A HttpSinkTask" should {

    "start" in {
      val task = new HttpSinkTask()
      val props = Map("http.api.url" -> "http://foo.bar").asJava

      task.start(props)

      task.writer.get mustBe a [HttpWriter]
    }

    "put" in {
      val writerMock = mock[HttpWriter]
      val task = new HttpSinkTask()
      task.writer = Some(writerMock)

      val records = List(new SinkRecord("topic", 0, null, null, null, "value", 0))
      task.put(records.asJava)

      verify(writerMock).put(eqTo(records))(any[ExecutionContext])
    }

    "put when null value" in {
      val writerMock = mock[HttpWriter]
      val task = new HttpSinkTask()
      task.writer = Some(writerMock)

      val records = List(new SinkRecord("topic", 0, null, null, null, null, 0))
      task.put(records.asJava)

      verify(writerMock, never).put(any[List[SinkRecord]])(any[ExecutionContext])
    }

    "flush" in {
      val writerMock = mock[HttpWriter]
      val task = new HttpSinkTask()
      task.writer = Some(writerMock)

      task.flush(Map[TopicPartition, OffsetAndMetadata]().asJava)

      verify(writerMock).flush
    }
  }
}
