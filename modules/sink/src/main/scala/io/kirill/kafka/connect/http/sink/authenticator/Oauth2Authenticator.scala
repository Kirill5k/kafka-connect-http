package io.kirill.kafka.connect.http.sink.authenticator

import java.time.Instant

import io.circe.generic.auto._
import io.circe.parser._
import io.kirill.kafka.connect.http.sink.HttpSinkConfig
import io.kirill.kafka.connect.http.sink.authenticator.Oauth2Authenticator.AuthToken
import io.kirill.kafka.connect.http.sink.errors.{AuthError, JsonParsingError}
import scalaj.http.Http

private[authenticator] final class Oauth2Authenticator(
    private val conf: HttpSinkConfig,
    private var authToken: AuthToken = AuthToken("expired", -1)
) extends Authenticator {
  import Oauth2Authenticator._

  override def authHeader(): String = {
    if (authToken.hasExpired) {
      refreshToken()
    }
    s"Bearer ${authToken.token}"
  }

  private val requestBody = List(
    "client_id"     -> conf.oauth2ClientId,
    "client_secret" -> conf.oauth2ClientSecret,
    "grant_type"    -> "client_credentials"
  )

  private def refreshToken(): Unit = {
    val response = Http(conf.oauth2TokenUrl)
      .header("Content-Type", "application/x-www-form-urlencoded")
      .auth(conf.oauth2ClientId, conf.oauth2ClientSecret)
      .postForm(requestBody)
      .asString

    if (response.isSuccess) {
      val json        = response.body
      val accessToken = decode[AccessTokenResponse](json).getOrElse(throw JsonParsingError(json))
      authToken = AuthToken(accessToken.access_token, accessToken.expires_in)
    } else {
      throw AuthError(s"error obtaining auth token. ${response.code}: ${response.body}")
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
    def apply(token: String, expiresIn: Long): AuthToken =
      AuthToken(token, Instant.now().plusSeconds(expiresIn))
  }
}
