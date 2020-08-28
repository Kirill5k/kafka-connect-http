package io.kirill.kafka.connect.http.sink.formatter

import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import org.apache.kafka.connect.sink.SinkRecord

trait Formatter {
  def toJson(records: Seq[SinkRecord]): String
}

final class RegexFormatter(
    private val config: HttpSinkConfig
) extends Formatter {

  private val regexReplacements =
    config.regexPatterns.zip(config.regexReplacements)

  private val formatRecord: SinkRecord => String = rec =>
    regexReplacements.foldLeft(rec.value().toString) {
      case (res, (regex, replacement)) => res.replaceAll(regex, replacement)
    }

  override def toJson(records: Seq[SinkRecord]): String =
    records
      .map(formatRecord)
      .mkString(config.batchPrefix, config.batchSeparator, config.batchSuffix)
}
