package io.kirill.kafka.connect.http.sink

import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HttpWriterSpec extends AnyWordSpec with Matchers {

  "A HttpWriter" should {

    "format sink records" in {
      val props = java.util.Map.of(
        "http.api.url", "http://foo.bar",
        "http.headers", "content-type:application/json|accept:application/json",
        "headers.separator", "\\|",
        "batch.size", "100",
        "batch.prefix", "[",
        "batch.suffix", "]",
        "max.retries", "3",
        "retry.backoff.ms", "10",
        "regex.patterns", ",~=~Struct\\{~\\}",
        "regex.replacements", "\",\"~\":\"~{\"~\"}",
      )

      val conf = HttpSinkConfig(props)
      val records = List(record("John Smith"), record("John Bloggs"))

      val formattedRecords = HttpWriter.formatRecords(conf, records)

      formattedRecords must be("""[{"name":"John Smith","age":"21"},{"name":"John Bloggs","age":"21"}]""")
    }
  }

  def record(name: String = "John Bloggs"): SinkRecord = {
    val schema = SchemaBuilder
      .struct()
      .name("com.example.Person")
      .field("name", Schema.STRING_SCHEMA)
      .field("age", Schema.INT32_SCHEMA)
      .build()
    val message = new Struct(schema)
      .put("name", name)
      .put("age", 21)
    new SinkRecord("egress", 1, null, "key", schema, message, 1)
  }
}
