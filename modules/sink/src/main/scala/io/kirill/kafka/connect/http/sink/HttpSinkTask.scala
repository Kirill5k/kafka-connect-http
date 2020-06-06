package io.kirill.kafka.connect.http.sink

import java.util

import info.BuildInfo
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

class HttpSinkTask extends SinkTask with Logging {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  var writer: Option[HttpWriter] = None

  override def start(props: util.Map[String, String]): Unit = {
    logger.info(s"starting http sink connector task: $props")
    writer = Some(HttpSinkConfig(props)).map(HttpWriter(_))
  }

  override def put(records: util.Collection[SinkRecord]): Unit = {
    logger.info(s"received ${records.size()} records")
    val recs = records.asScala.filter(r => r != null && r.value() != null).toList
    if (recs.nonEmpty) {
      writer.foreach(_.put(recs))
    }
  }

  override def stop(): Unit =
    logger.info(s"stopping http sink connector task")

  override def version(): String =
    BuildInfo.version

  override def flush(currentOffsets: util.Map[TopicPartition, OffsetAndMetadata]): Unit =
    writer.foreach(_.flush)
}
