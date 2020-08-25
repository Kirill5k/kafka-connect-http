package io.kirill.kafka.connect.http.sink.authenticator

import io.kirill.kafka.connect.http.sink.HttpSinkConfig

trait Authenticator {
  def authHeader(): String
}

object Authenticator {
  def oauth2(config: HttpSinkConfig): Authenticator =
    new Oauth2Authenticator(config)
}
