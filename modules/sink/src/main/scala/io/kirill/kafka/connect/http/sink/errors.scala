package io.kirill.kafka.connect.http.sink

object errors {

  sealed trait SinkError extends Throwable {
    val message: String
    override def getMessage: String = message
  }

  final case class AuthError(message: String) extends SinkError

  final case class JsonParsingError(json: String) extends SinkError {
    val message = s"error parsing json from a response: ${json}"
  }

  final case object MaxAmountOfRetriesReached extends SinkError {
    val message: String = "reached the maximum number of times to retry on errors before failing the task"
  }
}
