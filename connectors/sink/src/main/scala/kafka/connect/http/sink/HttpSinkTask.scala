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

import java.util

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

import scala.jdk.CollectionConverters._

class HttpSinkTask extends SinkTask with Logging {
  var writer: Option[HttpWriter] = None

  override def start(props: util.Map[String, String]): Unit = {
    logger.info(s"starting http sink connector task: $props")
    writer = Some(HttpSinkConfig(props)).map(HttpWriter.make)
  }

  override def put(records: util.Collection[SinkRecord]): Unit = {
    logger.trace(s"received ${records.size()} records")
    val recs = records.asScala.filter(r => r != null && r.value() != null).toList
    if (recs.nonEmpty) {
      writer.foreach(_.put(recs))
    }
  }

  override def stop(): Unit =
    logger.info(s"stopping http sink connector task")

  override def version(): String =
    getClass.getPackage.getImplementationVersion

  override def flush(currentOffsets: util.Map[TopicPartition, OffsetAndMetadata]): Unit =
    writer.foreach(_.flush())
}
