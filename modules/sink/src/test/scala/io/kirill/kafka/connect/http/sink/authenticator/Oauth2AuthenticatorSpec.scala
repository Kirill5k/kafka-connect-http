package io.kirill.kafka.connect.http.sink.authenticator

import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import io.kirill.kafka.connect.http.sink.authenticator.Oauth2Authenticator.AuthToken
import io.kirill.kafka.connect.http.sink.errors.{AuthError, JsonParsingError}
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client.HttpURLConnectionBackend

class Oauth2AuthenticatorSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val backend = HttpURLConnectionBackend()

  val config = HttpSinkConfig(
    Map(
      "http.api.url"              -> "http://localhost:12346/events",
      "schemas.enable"            -> "false",
      "auth.type"                 -> "oauth2",
      "auth.oauth2.client.id"     -> "client-id",
      "auth.oauth2.client.secret" -> "client-secret",
      "auth.oauth2.token.url"     -> "http://localhost:12346/token"
    )
  )

  "An Oauth2Authenticator" should {

    "return auth header if token is still valid" in withMockserver { server =>
      val authToken     = AuthToken("valid-token", 1000)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      authenticator.authHeader() must be("Bearer valid-token")
      server.verifyZeroInteractions()
    }

    "obtain new access token if current auth token has expire" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/token"))
        .respond(response().withBody(s"""
            |{"access_token": "new-token","expires_in": 7200,"token_type": "Application Access Token"}
            |""".stripMargin))

      val authToken     = AuthToken("expired-token", 0)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      authenticator.authHeader() must be("Bearer new-token")

      server.verify(
        request()
          .withHeader("Content-Type", "application/x-www-form-urlencoded")
          .withHeader("Authorization", "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=")
          .withBody("client_id=client-id&client_secret=client-secret&grant_type=client_credentials")
      )
    }

    "throw parsing error when unexpected response returned" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/token"))
        .respond(response().withBody(s"""{"foo": "bar"}""".stripMargin))

      val authToken     = AuthToken("expired-token", 0)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      assertThrows[JsonParsingError] {
        authenticator.authHeader()
      }
    }

    "throw auth error when fail response returned" in withMockserver { server =>
      server
        .when(request().withMethod("POST").withPath("/token"))
        .respond(response().withBody(s"""{"error": "invalid client id"}""".stripMargin).withStatusCode(401))

      val authToken     = AuthToken("expired-token", 0)
      val authenticator = new Oauth2Authenticator(config, backend, authToken)

      assertThrows[AuthError] {
        authenticator.authHeader()
      }
    }
  }

  def withMockserver(test: ClientAndServer => Any): Any = {
    val server = new ClientAndServer(12346)
    try {
      test(server)
    } finally {
      server.stop()
    }
  }
}
