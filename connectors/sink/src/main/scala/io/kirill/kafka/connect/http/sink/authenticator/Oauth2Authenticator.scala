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

package io.kirill.kafka.connect.http.sink.authenticator

import java.time.Instant

import io.circe.generic.auto._
import io.circe.parser._
import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import io.kirill.kafka.connect.http.sink.authenticator.Oauth2Authenticator.AuthToken
import io.kirill.kafka.connect.http.sink.errors.{AuthError, HttpClientError, JsonParsingError}
import sttp.client.{NothingT, SttpBackend, UriContext, basicRequest}

import scala.util.{Failure, Success, Try}

private[authenticator] final class Oauth2Authenticator(
    private val conf: HttpSinkConfig,
    private val backend: SttpBackend[Try, Nothing, NothingT],
    private var authToken: AuthToken = AuthToken("expired", -1)
) extends Authenticator {
  import Oauth2Authenticator._

  override def authHeader(): String = {
    if (authToken.hasExpired) {
      refreshToken()
    }
    s"Bearer ${authToken.token}"
  }

  private val requestBody = Map(
    "client_id"     -> conf.oauth2ClientId,
    "client_secret" -> conf.oauth2ClientSecret,
    "grant_type"    -> "client_credentials"
  )

  private def refreshToken(): Unit = {
    val response = backend.send(
      basicRequest
        .header("Content-Type", "application/x-www-form-urlencoded")
        .auth
        .basic(conf.oauth2ClientId, conf.oauth2ClientSecret)
        .post(uri"${conf.oauth2TokenUrl}")
        .body(requestBody)
    )

    response match {
      case Success(res) =>
        res.body match {
          case Right(json) =>
            val accessToken = decode[AccessTokenResponse](json).getOrElse(throw JsonParsingError(json))
            authToken = AuthToken(accessToken.access_token, accessToken.expires_in)
          case Left(error) =>
            throw AuthError(s"error obtaining auth token. $error")
        }
      case Failure(exception) =>
        throw HttpClientError(exception.getMessage)
    }
  }
}

private[authenticator] object Oauth2Authenticator {

  final case class AccessTokenResponse(
      access_token: String,
      expires_in: Long,
      token_type: String
  )

  final case class AuthToken(token: String, expiresAt: Instant) {
    def isValid: Boolean    = expiresAt.isAfter(Instant.now())
    def hasExpired: Boolean = !isValid
  }

  object AuthToken {
    def apply(token: String, expiresIn: Long, expirationPenalty: Long = 30): AuthToken =
      AuthToken(token, Instant.now().plusSeconds(expiresIn - expirationPenalty))
  }
}
