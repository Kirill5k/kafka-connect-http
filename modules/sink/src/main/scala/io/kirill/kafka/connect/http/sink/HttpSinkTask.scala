package io.kirill.kafka.connect.http.sink

import java.util

import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

class HttpSinkTask extends SinkTask with Logging {
  var sinkConfig: HttpSinkConfig = _


  override def start(props: util.Map[String, String]): Unit = {
    logger.info(s"starting http sink connector task: $props")
    sinkConfig = new HttpSinkConfig(props)
  }

  override def put(records: util.Collection[SinkRecord]): Unit = {
    logger.info(s"received ${records.size()} records")
    records.forEach(r => logger.info(s"${r.value()}"))
  }

  override def stop(): Unit = {
    logger.info(s"stopping http sink connector task")
  }

  override def version(): String =
    AppInfoParser.getVersion
//    getClass.getPackage.getImplementationVersion
}
