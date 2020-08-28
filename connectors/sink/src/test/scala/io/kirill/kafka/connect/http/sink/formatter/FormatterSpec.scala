package io.kirill.kafka.connect.http.sink.formatter

import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FormatterSpec extends AnyWordSpec with Matchers {
  "A RegexFormatter" should {

    "convert records to a json string based on regex" in {
      val config = HttpSinkConfig(Map(
        "http.api.url" -> "http://localhost:8080/events",
        "retry.backoff.ms" -> "10",
        "regex.patterns" -> ",~=~Struct\\{~\\}",
        "regex.replacements" -> "\",\"~\":\"~{\"~\"}",
        "batch.prefix" -> "[",
        "batch.suffix" -> "]",
        "atch.separator" -> ","
      ))

      val formatter = Formatter.regexBased(config)

      val json = formatter.toJson(List(record("john bloggs"), record("john smith")))

      json must be("""[{"name":"john bloggs","age":"21"},{"name":"john smith","age":"21"}]""")
    }
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
}
