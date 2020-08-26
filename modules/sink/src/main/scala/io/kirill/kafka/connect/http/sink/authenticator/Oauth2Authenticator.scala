package io.kirill.kafka.connect.http.sink.authenticator

import java.time.Instant

import io.circe.generic.auto._
import io.circe.parser._
import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import io.kirill.kafka.connect.http.sink.authenticator.Oauth2Authenticator.AuthToken
import io.kirill.kafka.connect.http.sink.errors.{AuthError, JsonParsingError}
import sttp.client.{Identity, NothingT, SttpBackend, UriContext, basicRequest}

private[authenticator] final class Oauth2Authenticator(
    private val conf: HttpSinkConfig,
    private val backend: SttpBackend[Identity, Nothing, NothingT],
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
    val response = backend.send(basicRequest
      .header("Content-Type", "application/x-www-form-urlencoded")
      .auth.basic(conf.oauth2ClientId, conf.oauth2ClientSecret)
      .post(uri"${conf.oauth2TokenUrl}")
      .body(requestBody))

    response.body match {
      case Right(json) =>
        val accessToken = decode[AccessTokenResponse](json).getOrElse(throw JsonParsingError(json))
        authToken = AuthToken(accessToken.access_token, accessToken.expires_in)
      case Left(error) =>
        throw AuthError(s"error obtaining auth token. $error")
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
