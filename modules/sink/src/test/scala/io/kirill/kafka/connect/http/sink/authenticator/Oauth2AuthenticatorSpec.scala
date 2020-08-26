package io.kirill.kafka.connect.http.sink.authenticator

import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import io.kirill.kafka.connect.http.sink.authenticator.Oauth2Authenticator.AuthToken
import io.kirill.kafka.connect.http.sink.errors.{AuthError, JsonParsingError}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client.testing.SttpBackendStub
import sttp.client.{Response, StringBody, SttpClientException}
import sttp.model.{Header, Method, StatusCode}

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
      val backend = SttpBackendStub.synchronous
        .whenAnyRequest
        .thenRespond(throw new SttpClientException.ConnectException(new RuntimeException))

      val authToken     = AuthToken("valid-token", 1000)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      authenticator.authHeader() must be("Bearer valid-token")
    }

    "obtain new access token if current auth token has expire" in {
      val backend = SttpBackendStub.synchronous
        .whenRequestMatches { r =>
          r.method == Method.POST &&
            r.headers.contains(Header("Content-Type", "application/x-www-form-urlencoded")) &&
            r.headers.contains(Header("Authorization", "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=")) &&
            r.body.asInstanceOf[StringBody].s == "client_id=client-id&client_secret=client-secret&grant_type=client_credentials"
        }
        .thenRespond("""{"access_token": "new-token","expires_in": 7200,"token_type": "Application Access Token"}""")

      val authToken     = AuthToken("expired-token", 0)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      authenticator.authHeader() must be("Bearer new-token")
    }

    "throw parsing error when unexpected response returned" in {
      val backend = SttpBackendStub.synchronous
        .whenAnyRequest
        .thenRespond("""{"foo": "bar"}""")

      val authToken     = AuthToken("expired-token", 0)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      assertThrows[JsonParsingError] {
        authenticator.authHeader()
      }
    }

    "throw auth error when fail response returned" in {
      val backend = SttpBackendStub.synchronous
        .whenAnyRequest
        .thenRespond(Response("error", StatusCode.InternalServerError))

      val authToken     = AuthToken("expired-token", 0)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      assertThrows[AuthError] {
        authenticator.authHeader()
      }
    }
  }
}
