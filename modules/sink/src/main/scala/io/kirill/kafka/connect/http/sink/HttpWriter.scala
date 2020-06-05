package io.kirill.kafka.connect.http.sink

import io.kirill.kafka.connect.http.sink.errors.MaxAmountOfRetriesReached
import org.apache.kafka.connect.sink.SinkRecord
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

final class HttpWriter(val conf: HttpSinkConfig) extends Logging {

  var currentBatch: Seq[SinkRecord] = List()
  var failedAttempts: Int = 0

  def put(records: Seq[SinkRecord])(implicit ec: ExecutionContext): Unit = {
    if (currentBatch.size + records.size >= conf.batchSize) {
      val (batch, remaining) = (currentBatch ++ records).splitAt(conf.batchSize)
      send(batch)
      put(remaining)
    } else {
      currentBatch = currentBatch ++ records
    }
  }

  def flush(implicit ec: ExecutionContext): Unit = {
    send(currentBatch)
    currentBatch = List()
  }

  private def send(records: Seq[SinkRecord])(implicit ec: ExecutionContext): Future[Unit] = {
    Future(HttpWriter.formatRecords(conf, records))
      .map(req => HttpWriter.sendRequest(conf, req))
      .flatMap { res =>
        if (res.is2xx) Future.successful(())
        else Future(logger.error(s"error sending records batch. code - ${res.code}. response - ${res.body}"))
          .flatMap(_ => retry(records))
      }
  }

  private def retry(records: Seq[SinkRecord])(implicit ec: ExecutionContext): Future[Unit] = {
    failedAttempts += 1
    if (failedAttempts <= conf.maxRetries) {
      Future(Thread.sleep(conf.retryBackoff)).flatMap(_ => send(records))
    } else {
      Future.failed(MaxAmountOfRetriesReached)
    }
  }
}

object HttpWriter {

  def sendRequest(conf: HttpSinkConfig, body: String): HttpResponse[String] = {
    Http(conf.httpApiUrl)
      .postData(body)
      .method(conf.httpRequestMethod)
      .headers(conf.httpHeaders.map(_.split(":")).map(x => (x(0), x(1))))
      .asString
  }

  def formatRecords(conf: HttpSinkConfig, records: Seq[SinkRecord]): String = {
    val regexReplacements = conf.regexPatterns.zip(conf.regexReplacements)
    val formatRecord: SinkRecord => String = rec => regexReplacements.foldLeft(rec.value().toString) {
      case (res, (regex, replacement)) => res.replaceAll(regex, replacement)
    }

    records.map(formatRecord).mkString(conf.batchPrefix, conf.batchSeparator, conf.batchSuffix)
  }

  def apply(conf: HttpSinkConfig): HttpWriter = new HttpWriter(conf)
}
