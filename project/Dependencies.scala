import sbt._

object Dependencies {

  object Versions {
    lazy val kafka = "2.5.0"
    lazy val confluent = "5.5.0"
    lazy val scalaj = "2.4.2"

    lazy val scalatest = "3.1.1"
    lazy val mockito = "1.14.0"
    lazy val mockWebServer = "3.14.4"
  }

  object Libraries {
    lazy val connectApi = "org.apache.kafka" % "connect-api" % Versions.kafka
    lazy val connectAvro = "io.confluent" % "kafka-connect-avro-converter" % Versions.confluent
    lazy val connectJson = "org.apache.kafka" % "connect-json" % Versions.kafka
    lazy val scalajHttp = "org.scalaj" %% "scalaj-http" % Versions.scalaj

    lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest
    lazy val mockitoCore = "org.mockito" %% "mockito-scala" % Versions.mockito
    lazy val mockitoScalatest = "org.mockito" %% "mockito-scala-scalatest" % Versions.mockito
    lazy val mockWebServer = "com.squareup.okhttp3" % "mockwebserver" % Versions.mockWebServer
  }

  lazy val sink = Seq(
    Libraries.connectApi,
    Libraries.connectAvro,
    Libraries.connectJson,
    Libraries.scalajHttp
  )

  lazy val test = Seq(
    Libraries.scalaTest % Test,
    Libraries.mockitoCore % Test,
    Libraries.mockitoScalatest % Test,
    Libraries.mockWebServer % Test
  )
}
