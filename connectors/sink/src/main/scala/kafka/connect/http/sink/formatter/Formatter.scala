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

package kafka.connect.http.sink.formatter

import kafka.connect.http.sink.HttpSinkConfig
import org.apache.kafka.connect.sink.SinkRecord

trait Formatter {
  def toJson(records: Iterable[SinkRecord]): String
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

  override def toJson(records: Iterable[SinkRecord]): String =
    records
      .map(formatRecord)
      .mkString(config.batchPrefix, config.batchSeparator, config.batchSuffix)
}

object Formatter {

  def regexBased(config: HttpSinkConfig): Formatter =
    new RegexFormatter(config)
}
