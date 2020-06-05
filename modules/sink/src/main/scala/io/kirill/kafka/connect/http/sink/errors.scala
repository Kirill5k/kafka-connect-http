package io.kirill.kafka.connect.http.sink

object errors {

  sealed trait SinkError extends Throwable {
    val message: String
    override def getMessage: String = message
  }

  final case object MaxAmountOfRetriesReached extends SinkError {
    val message: String = "reached the maximum number of times to retry on errors before failing the task"
  }
}
