import sbt._

object Dependencies {
  object Versions {
    lazy val kafka     = "2.5.0"
    lazy val confluent = "5.5.0"

    lazy val scalatest = "3.1.1"
  }

  object Libraries {
    lazy val connectApi           = "org.apache.kafka" % "connect-api"                  % Versions.kafka
    lazy val connectAvroConverter = "io.confluent"     % "kafka-connect-avro-converter" % Versions.confluent

    lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest
  }

  lazy val sink = Seq(
    Libraries.connectApi,
    Libraries.connectAvroConverter
  )

  lazy val test = Seq(
    Libraries.scalaTest % Test
  )
}
