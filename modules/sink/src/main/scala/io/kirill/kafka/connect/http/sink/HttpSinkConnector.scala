package io.kirill.kafka.connect.http.sink

import java.util

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector

import scala.jdk.CollectionConverters._

class HttpSinkConnector extends SinkConnector with Logging {
  private var sinkConfig: HttpSinkConfig = _

  override def start(props: util.Map[String, String]): Unit = {
    log.info(s"starting http sink connector: $props")
    sinkConfig = new HttpSinkConfig(props)
  }

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    log.info(s"setting task configurations for $maxTasks worker")
    List.fill(maxTasks)(sinkConfig.props).asJava
  }

  override def version(): String =
    AppInfoParser.getVersion

  override def stop(): Unit = {
    log.info(s"stopping http sink connector")
  }

  override def config(): ConfigDef = HttpSinkConfig.DEF
  override def taskClass(): Class[_ <: Task] = classOf[HttpSinkTask]
}
