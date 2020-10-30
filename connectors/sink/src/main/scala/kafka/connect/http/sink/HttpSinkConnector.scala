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

package kafka.connect.http.sink

import java.util

import org.apache.kafka.common.config.ConfigDef
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
    getClass.getPackage.getImplementationVersion

  override def stop(): Unit =
    logger.info(s"stopping http sink connector")

  override def config(): ConfigDef           = HttpSinkConfig.DEF
  override def taskClass(): Class[_ <: Task] = classOf[HttpSinkTask]
}
