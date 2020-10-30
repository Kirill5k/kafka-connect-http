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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class HttpSinkConnectorSpec extends AnyWordSpec with Matchers {

  "A HttpSinkConnector" should {
    val props     = Map("http.api.url" -> "http://foo.bar").asJava
    val connector = new HttpSinkConnector()

    "start & taskConfigs" in {
      connector.start(props)

      connector.taskConfigs(3) must be(List(props, props, props).asJava)
    }

    "config" in {
      connector.config() must be(HttpSinkConfig.DEF)
    }

    "taskClass" in {
      connector.taskClass() must be(classOf[HttpSinkTask])
    }
  }
}
