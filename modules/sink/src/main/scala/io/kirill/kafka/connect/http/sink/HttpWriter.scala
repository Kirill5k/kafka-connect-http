package io.kirill.kafka.connect.http.sink

import java.time.Instant

import io.kirill.kafka.connect.http.sink.authenticator.Authenticator
import io.kirill.kafka.connect.http.sink.dispatcher.Dispatcher
import org.apache.kafka.connect.sink.SinkRecord
import sttp.client.TryHttpURLConnectionBackend

class HttpWriter(
    private val config: HttpSinkConfig,
    private val dispatcher: Dispatcher,
    private val authenticator: Option[Authenticator]
) extends Logging {

  var currentBatch: List[SinkRecord] = List()
  var time: Long                     = Instant.now.toEpochMilli

  def put(records: List[SinkRecord]): Unit = {
    val currentTime = Instant.now().toEpochMilli
    if (currentBatch.size + records.size >= config.batchSize || currentTime - time >= config.batchIntervalMs) {
      time = currentTime
      val (batch, remaining) = (currentBatch ++ records).splitAt(config.batchSize)
      sendBatch(batch)
      if (remaining.nonEmpty) {
        put(remaining)
      }
    } else {
      currentBatch = currentBatch ++ records
    }
  }

  def flush(): Unit = {
    sendBatch(currentBatch)
    currentBatch = List()
  }

  private def sendBatch(records: List[SinkRecord]): Unit = {
    val body    = HttpWriter.formatRecords(config, records)
    val headers = authenticator.fold(config.httpHeaders)(a => config.httpHeaders + ("Authorization" -> a.authHeader()))
    dispatcher.send(headers, body)
  }
}

object HttpWriter {

  def formatRecords(conf: HttpSinkConfig, records: Seq[SinkRecord]): String = {
    val regexReplacements = conf.regexPatterns.zip(conf.regexReplacements)
    val formatRecord: SinkRecord => String = rec =>
      regexReplacements.foldLeft(rec.value().toString) {
        case (res, (regex, replacement)) => res.replaceAll(regex, replacement)
      }

    records.map(formatRecord).mkString(conf.batchPrefix, conf.batchSeparator, conf.batchSuffix)
  }

  def make(config: HttpSinkConfig): HttpWriter = {
    val backend = TryHttpURLConnectionBackend()
    val dispatcher = Dispatcher.sttp(config, backend)
    val authenticator = config.authType match {
      case "oauth2" => Some(Authenticator.oauth2(config, backend))
      case _ => None
    }
    new HttpWriter(config, dispatcher, authenticator)
  }
}
