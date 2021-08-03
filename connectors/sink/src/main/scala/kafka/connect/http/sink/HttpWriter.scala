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

import java.util.Optional

import kafka.connect.http.sink.authenticator.Authenticator
import kafka.connect.http.sink.dispatcher.Dispatcher
import kafka.connect.http.sink.errors.{MaxAmountOfRetriesReached, SinkError}
import kafka.connect.http.sink.formatter.Formatter
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.{SinkRecord, SinkTaskContext}
import sttp.client3.{SttpBackendOptions, TryHttpURLConnectionBackend}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}

class HttpWriter(
    private val config: HttpSinkConfig,
    private val dispatcher: Dispatcher,
    private val formatter: Formatter,
    private val authenticator: Option[Authenticator],
    private val context: Option[SinkTaskContext]
)(implicit ec: ExecutionContext)
    extends Logging {

  var batches: List[SinkRecord]                                     = List()
  val lastCommitted: mutable.Map[TopicPartition, OffsetAndMetadata] = mutable.Map.empty

  def put(records: List[SinkRecord]): Unit = {
    batches = batches ++ records
    // send only one batch in batches
    if (batches.size >= config.batchSize || lastCommitted.isEmpty) {
      val (toSend, remaining) = batches.splitAt(config.batchSize)
      sendBatch(toSend, true)
      batches = remaining
    }
  }

  def flush(): Map[TopicPartition, OffsetAndMetadata] = {
    batches.grouped(config.batchSize).foreach(sendBatch(_, false))
    batches = List()
    lastCommitted.toMap
  }

  private def sendBatch(records: List[SinkRecord], failFast: Boolean): Unit = {
    val body = formatter.toOutputFormat(records)
    val headers =
      authenticator.fold(config.httpHeaders)(a => config.httpHeaders + (config.authHeaderName -> a.authHeader()))
    dispatcher.send(headers, body, failFast) match {
      case None =>
        records.foreach(r => updateLastCommitted(toTopicPartition(r), r.kafkaOffset()))
      case Some(e) => pauseFor(e, records.map(toTopicPartition), dispatcher.pauseTime)
    }
  }

  private def pauseFor(e: SinkError, tp: List[TopicPartition], duration: Duration): Unit =
    e match {
      case er: MaxAmountOfRetriesReached => throw er
      case _ =>
        context.foreach(_.pause(tp: _*))
        logger.info(s"Pausing partitions ${tp.mkString(",")} for ${duration}")
        unpause(tp, duration).onComplete(_ => logger.info(s"Partitions un-paused => ${tp.mkString(",")}"))
    }

  private def unpause(tp: List[TopicPartition], duration: Duration) = Future {
    Thread.sleep(duration.toMillis)
    context.foreach(_.resume(tp: _*))
  }

  private def updateLastCommitted(tp: TopicPartition, offset: Long) = {
    val of = lastCommitted
      .get(tp)
      .filter(p => p.offset() < offset)
      .getOrElse(
        new OffsetAndMetadata(offset, Optional.empty(), null)
      )
    lastCommitted.put(tp, of)
  }

  private def toTopicPartition(r: SinkRecord) = new TopicPartition(r.topic(), r.kafkaPartition())

}

object HttpWriter {

  def make(context: Option[SinkTaskContext])(config: HttpSinkConfig)(implicit ec: ExecutionContext): HttpWriter = {
    val backend    = TryHttpURLConnectionBackend(options = SttpBackendOptions(connectionTimeout = config.connectTimeout.milliseconds, None))
    val dispatcher = Dispatcher.sttp(config, backend)
    val formatter = config.formatter match {
      case "regex"  => Formatter.regexBased(config)
      case "schema" => Formatter.schemaBased(config)
    }
    val authenticator = config.authType match {
      case "oauth2" => Some(Authenticator.oauth2(config, backend))
      case _        => None
    }
    new HttpWriter(config, dispatcher, formatter, authenticator, context)
  }
}
