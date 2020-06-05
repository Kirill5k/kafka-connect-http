package io.kirill.kafka.connect.http.sink

import java.util

import info.BuildInfo
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.utils.AppInfoParser
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector

import scala.jdk.CollectionConverters._

class HttpSinkConnector extends SinkConnector with Logging {
  private var sinkConfig: HttpSinkConfig = _

  override def start(props: util.Map[String, String]): Unit = {
    logger.info(s"starting http sink connector: $props")
    sinkConfig = new HttpSinkConfig(props)
  }

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    logger.info(s"setting task configurations for $maxTasks worker")
    List.fill(maxTasks)(sinkConfig.props).asJava
  }

  override def version(): String =
    BuildInfo.version

  override def stop(): Unit = {
    logger.info(s"stopping http sink connector")
  }

  override def config(): ConfigDef = HttpSinkConfig.DEF
  override def taskClass(): Class[_ <: Task] = classOf[HttpSinkTask]
}
