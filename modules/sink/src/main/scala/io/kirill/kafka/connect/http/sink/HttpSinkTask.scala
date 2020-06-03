package io.kirill.kafka.connect.http.sink

import java.util

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

import scala.jdk.CollectionConverters._

class HttpSinkTask extends SinkTask with Logging {
  var sinkConfig: HttpSinkConfig = _
  var writer: HttpWriter = _

  override def start(props: util.Map[String, String]): Unit = {
    logger.info(s"starting http sink connector task: $props")
    sinkConfig = new HttpSinkConfig(props)
    writer = new HttpWriter(sinkConfig)
  }

  override def put(records: util.Collection[SinkRecord]): Unit = {
    logger.info(s"received ${records.size()} records")
    writer.put(records.asScala.toList)
  }

  override def stop(): Unit = {
    logger.info(s"stopping http sink connector task")
  }

  override def version(): String =
    AppInfoParser.getVersion

  override def flush(currentOffsets: util.Map[TopicPartition, OffsetAndMetadata]): Unit =
    writer.flush()
}
