package io.kirill.kafka.connect.http.sink.dispatcher

import io.kirill.kafka.connect.http.sink.errors.{HttpClientError, MaxAmountOfRetriesReached}
import io.kirill.kafka.connect.http.sink.{HttpSinkConfig, Logging}
import sttp.client._
import sttp.model.Method

import scala.util.{Failure, Success, Try}

trait Dispatcher extends Logging {
  def send(headers: Map[String, String], body: String): Unit
}

private[dispatcher] final class SttpDispatcher(
    private val config: HttpSinkConfig,
    private val backend: SttpBackend[Try, Nothing, NothingT],
    private var failedAttempts: Int = 0
) extends Dispatcher {

  override def send(headers: Map[String, String], body: String): Unit = {
    val response = sendRequest(headers, body)
    if (!response.isSuccess) {
      logger.error(s"error dispatching data. ${response.code.code}: ${response.body.fold(s => s, s => s)}")
      retry(headers, body)
    }
  }

  private def retry(headers: Map[String, String], body: String): Unit = {
    failedAttempts += 1
    if (failedAttempts <= config.maxRetries) {
      Thread.sleep(config.retryBackoff)
      send(headers, body)
    } else {
      throw MaxAmountOfRetriesReached
    }
  }

  private def sendRequest(headers: Map[String, String], body: String): Response[Either[String, String]] =
    backend.send(
      basicRequest
        .headers(headers)
        .body(body)
        .method(Method(config.httpRequestMethod), uri"${config.httpApiUrl}")
    ) match {
      case Success(value) => value
      case Failure(exception) => throw HttpClientError(exception.getMessage)
    }
}

object Dispatcher {
  def sttp(config: HttpSinkConfig, backend: SttpBackend[Try, Nothing, NothingT]): Dispatcher =
    new SttpDispatcher(config, backend)
}
