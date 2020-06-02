package io.kirill.kafka.connect.http.sink

import java.util

import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

class HttpSinkTask extends SinkTask with Logging {
  override def start(props: util.Map[String, String]): Unit = ???

  override def put(records: util.Collection[SinkRecord]): Unit = ???

  override def stop(): Unit = ???

  override def version(): String = ???
}
