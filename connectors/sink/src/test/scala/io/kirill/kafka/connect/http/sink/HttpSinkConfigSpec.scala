package io.kirill.kafka.connect.http.sink

import org.apache.kafka.common.config.ConfigException
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class HttpSinkConfigSpec extends AnyWordSpec with Matchers {

  "A HttpSinkConfig" should {
    "return error if api url is not provided" in {
      a[ConfigException] must be thrownBy (new HttpSinkConfig(Map[String, String]().asJava))
    }

    "return default values for not provided props" in {
      val props = Map("http.api.url" -> "http://foo.bar")

      val config = new HttpSinkConfig(props.asJava)

      config.httpApiUrl must be("http://foo.bar")
      config.httpRequestMethod must be("POST")
      config.httpHeaders must be(Nil)
      config.batchSize must be(1)
      config.batchIntervalMs must be(Long.MaxValue)
      config.batchPrefix must be("")
      config.batchSuffix must be("")
      config.batchSeparator must be(",")
      config.maxRetries must be(10)
      config.retryBackoff must be(3000)
      config.regexPatterns must be(List(""))
      config.regexReplacements must be(List(""))

      config.authType must be("none")
      config.oauth2TokenUrl must be("")
      config.oauth2ClientId must be("")
      config.oauth2ClientSecret must be("")
    }

    "set correct props" in {
      val props = Map(
        "http.api.url"        -> "http://foo.bar",
        "http.request.method" -> "PUT",
        "http.headers"        -> "content-type:application/json|accept:application/json",
        "headers.separator"   -> "\\|",
        "batch.size"          -> "100",
        "batch.interval.ms"   -> "100",
        "batch.prefix"        -> "[",
        "batch.suffix"        -> "]",
        "batch.separator"     -> ";",
        "max.retries"         -> "3",
        "retry.backoff.ms"    -> "10",
        "regex.patterns"      -> "^foo~bar$",
        "regex.replacements"  -> "bar~foo",
        "regex.separator"     -> "~",
        "auth.type"           -> "oauth2",
        "auth.oauth2.client.id"      -> "client-id",
        "auth.oauth2.client.secret"  -> "client-secret",
        "auth.oauth2.token.url"      -> "http://foo.bar/token"
      )

      val config = new HttpSinkConfig(props.asJava)

      config.httpApiUrl must be("http://foo.bar")
      config.httpRequestMethod must be("PUT")
      config.httpHeaders must be(Map("content-type" -> "application/json", "accept" -> "application/json"))
      config.batchSize must be(100)
      config.batchIntervalMs must be (100)
      config.batchPrefix must be("[")
      config.batchSuffix must be("]")
      config.batchSeparator must be(";")
      config.maxRetries must be(3)
      config.retryBackoff must be(10)
      config.regexPatterns must be(List("^foo", "bar$"))
      config.regexReplacements must be(List("bar", "foo"))

      config.authType must be("oauth2")
      config.oauth2TokenUrl must be("http://foo.bar/token")
      config.oauth2ClientId must be("client-id")
      config.oauth2ClientSecret must be("client-secret")
    }
  }
}
