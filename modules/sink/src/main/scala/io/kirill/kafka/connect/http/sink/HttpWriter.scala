package io.kirill.kafka.connect.http.sink

import org.apache.kafka.connect.sink.SinkRecord

final class HttpWriter(val config: HttpSinkConfig) {

  var currentBatch: Seq[SinkRecord] = List()

  def put(records: Seq[SinkRecord]): Unit = {
    if (currentBatch.size + records.size >= config.batchSize) {
      val (batch, remaining) = (currentBatch ++ records).splitAt(config.batchSize)
      send(batch)
      currentBatch = remaining
    } else {
      currentBatch = currentBatch ++ records
    }
  }

  def flush(): Unit = {
    send(currentBatch)
    currentBatch = List()
  }

  def send(records: Seq[SinkRecord]): Unit = {

  }
}
