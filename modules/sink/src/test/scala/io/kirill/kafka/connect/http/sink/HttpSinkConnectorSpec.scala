package io.kirill.kafka.connect.http.sink

import org.apache.kafka.connect.sink.SinkConnector
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

    "version" in {
      connector.version() must be("0.0.1-SNAPSHOT")
    }

    "config" in {
      connector.config() must be(HttpSinkConfig.DEF)
    }

    "taskClass" in {
      connector.taskClass() must be(classOf[HttpSinkTask])
    }
  }
}
