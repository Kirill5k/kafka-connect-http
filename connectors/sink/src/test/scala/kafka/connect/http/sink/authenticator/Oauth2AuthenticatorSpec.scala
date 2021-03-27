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

package kafka.connect.http.sink.authenticator

import kafka.connect.http.sink.HttpSinkConfig
import kafka.connect.http.sink.authenticator.Oauth2Authenticator.AuthToken
import kafka.connect.http.sink.errors.{AuthError, HttpClientError, JsonParsingError}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Response, StringBody, SttpClientException}
import sttp.model.{Header, Method, StatusCode}
import sttp.monad.TryMonad

import scala.util.Try

class Oauth2AuthenticatorSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val config = HttpSinkConfig(
    Map(
      "http.api.url"              -> "http://localhost:8080/events",
      "schemas.enable"            -> "false",
      "auth.type"                 -> "oauth2",
      "auth.oauth2.client.id"     -> "client-id",
      "auth.oauth2.client.secret" -> "client-secret",
      "auth.oauth2.token.url"     -> "http://localhost:8080/token"
    )
  )

  "An Oauth2Authenticator" should {

    "return auth header if token is still valid" in {
      val backend = SttpBackendStub[Try, Any](TryMonad).whenRequestMatchesPartial { case r =>
        throw new SttpClientException.ConnectException(r, new RuntimeException())
      }

      val authToken     = AuthToken("valid-token", 1000)
      val authenticator = new Oauth2Authenticator(config, backend, Some(authToken))

      authenticator.authHeader() must be("Bearer valid-token")
    }

    "obtain new access token if current auth token has expire" in {
      val backend = SttpBackendStub[Try, Any](TryMonad)
        .whenRequestMatches { r =>
          r.method == Method.POST &&
          r.headers.contains(Header("Content-Type", "application/x-www-form-urlencoded")) &&
          r.headers.contains(Header("Authorization", "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=")) &&
          r.body
            .asInstanceOf[StringBody]
            .s == "client_id=client-id&client_secret=client-secret&grant_type=client_credentials"
        }
        .thenRespond("""{"access_token": "new-token","expires_in": 7200,"token_type": "Application Access Token"}""")

      val authToken     = AuthToken("expired-token", 0)
      val authenticator = new Oauth2Authenticator(config, backend, Some(authToken))

      authenticator.authHeader() must be("Bearer new-token")
    }

    "throw parsing error when unexpected response returned" in {
      val backend = SttpBackendStub[Try, Any](TryMonad).whenAnyRequest
        .thenRespond("""{"foo": "bar"}""")

      val authenticator = new Oauth2Authenticator(config, backend, None)

      assertThrows[JsonParsingError] {
        authenticator.authHeader()
      }
    }

    "throw auth error when fail response returned" in {
      val backend = SttpBackendStub[Try, Any](TryMonad).whenAnyRequest
        .thenRespond(Response("error-message", StatusCode.InternalServerError))

      val authenticator = new Oauth2Authenticator(config, backend, None)

      val error = intercept[AuthError] {
        authenticator.authHeader()
      }

      error.message must be("error obtaining auth token. 500 - error-message")
    }

    "throw http client error when request fails" in {
      val backend = SttpBackendStub[Try, Any](TryMonad)
        .whenRequestMatchesPartial { case r =>
          throw new SttpClientException.ConnectException(r, new RuntimeException("runtime error"))
        }

      val authenticator = new Oauth2Authenticator(config, backend, None)

      val error = intercept[HttpClientError] {
        authenticator.authHeader()
      }

      error.message must be("runtime error")
    }
  }
}
