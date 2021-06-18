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
import org.apache.kafka.connect.data.{Schema, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import java.lang.{String => JavaString}

import scala.util.{Failure, Try}

trait Formatter {
  def toOutputFormat(records: Seq[SinkRecord]): String
}

final class RegexFormatter(
    private val config: HttpSinkConfig
) extends Formatter {

  private val regexReplacements =
    config.regexPatterns.zip(config.regexReplacements)

  private val formatRecord: SinkRecord => String = rec =>
    regexReplacements.foldLeft(rec.value().toString) { case (res, (regex, replacement)) =>
      res.replaceAll(regex, replacement)
    }

  override def toOutputFormat(records: Seq[SinkRecord]): String =
    records
      .map(formatRecord)
      .mkString(config.batchPrefix, config.batchSeparator, config.batchSuffix)
}

final class SchemaBasedFormatter(private val config: HttpSinkConfig) extends Formatter {

  override def toOutputFormat(records: Seq[SinkRecord]): String =
    records
      .filter(_.value() != null)
      .map(formatRecord)
      .mkString(config.batchPrefix, config.batchSeparator, config.batchSuffix)

  private val formatRecord: SinkRecord => String = rec =>
    rec.valueSchema().`type`() match {
      case Schema.Type.STRUCT => formatStruct(rec.valueSchema(), rec.value().asInstanceOf[Struct])
      // assuming that bytes are actually convertible to string
      case Schema.Type.BYTES =>
        Try(new JavaString(rec.value().asInstanceOf[Array[Byte]])).getOrElse(JavaString.valueOf(rec.value))
      case Schema.Type.STRING => rec.value().asInstanceOf[JavaString]
      case _                  => JavaString.valueOf(rec.value())
    }

  // TODO: improve
  private def formatStruct(schema: Schema, value: Struct): String = JavaString.valueOf(value)

}

object Formatter {

  def regexBased(config: HttpSinkConfig): Formatter =
    new RegexFormatter(config)

  def schemaBased(config: HttpSinkConfig): Formatter =
    new SchemaBasedFormatter(config)
}
