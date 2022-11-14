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

import java.nio.charset.Charset

import kafka.connect.http.sink.HttpSinkConfig
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FormatterSpec extends AnyWordSpec with Matchers {
  "A RegexFormatter" should {

    "convert records to a json string based on regex" in {
      val config = HttpSinkConfig(
        Map(
          "http.api.url"       -> "http://localhost:8080/events",
          "retry.backoff.ms"   -> "10",
          "regex.patterns"     -> ",~=~Struct\\{~\\}",
          "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
          "batch.prefix"       -> "[",
          "batch.suffix"       -> "]",
          "atch.separator"     -> ","
        )
      )

      val formatter = Formatter.regexBased(config)

      val json = formatter.toOutputFormat(List(record("john bloggs"), record("john smith")))

      json must be("""[{"name":"john bloggs","age":"21"},{"name":"john smith","age":"21"}]""")
    }
  }

  "convert records to a json using schema" in {
    val config = HttpSinkConfig(
      Map(
        "http.api.url"     -> "http://localhost:8080/events",
        "retry.backoff.ms" -> "10",
        "formatter.type"   -> "schema",
        "batch.prefix"     -> "[",
        "batch.suffix"     -> "]",
        "batch.separator"  -> ","
      )
    )

    val formatter = Formatter.schemaBased(config)

    val json = formatter.toOutputFormat(List(binary("john bloggs"), binary("john smith")))

    json must be("""[john bloggs,john smith]""")
  }

  def record(name: String): SinkRecord = {
    val schema = SchemaBuilder
      .struct()
      .name("com.example.Person")
      .field("name", Schema.STRING_SCHEMA)
      .field("age", Schema.INT32_SCHEMA)
      .build()
    val message = new Struct(schema)
      .put("name", name)
      .put("age", 21)
    new SinkRecord("topic", 1, null, "key", schema, message, 1)
  }

  def binary(name: String): SinkRecord = {
    val schema  = Schema.BYTES_SCHEMA
    val message = new String(name).getBytes(Charset.defaultCharset())
    new SinkRecord("topic", 1, null, "key", schema, message, 1)
  }
}
