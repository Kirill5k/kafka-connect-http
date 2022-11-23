import sbt._

object Dependencies {

  object Versions {
    val kafka     = "3.3.1"
    val confluent = "7.3.0"
    val circe     = "0.14.3"
    val sttp      = "3.8.3"

    val scalatest   = "3.2.14"
    val mockito     = "1.17.12"
    val mockserver  = "5.14.0"
    val scalaCompat = "2.8.1"
  }

  object Libraries {
    val scalaCompat       = "org.scala-lang.modules"        %% "scala-collection-compat"      % Versions.scalaCompat
    val connectAvro       = "io.confluent"                   % "kafka-connect-avro-converter" % Versions.confluent
    val connectApi        = "org.apache.kafka"               % "connect-api"                  % Versions.kafka
    val connectJson       = "org.apache.kafka"               % "connect-json"                 % Versions.kafka
    val sttpCore          = "com.softwaremill.sttp.client3" %% "core"                         % Versions.sttp
    val sttpFutureBackend = "com.softwaremill.sttp.client3" %% "circe"                        % Versions.sttp

    val circeCore    = "io.circe" %% "circe-core"    % Versions.circe
    val circeParser  = "io.circe" %% "circe-parser"  % Versions.circe
    val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe

    val scalaTest        = "org.scalatest"  %% "scalatest"               % Versions.scalatest
    val mockitoCore      = "org.mockito"    %% "mockito-scala"           % Versions.mockito
    val mockitoScalatest = "org.mockito"    %% "mockito-scala-scalatest" % Versions.mockito
  }

  val sink = Seq(
    Libraries.scalaCompat,
    Libraries.connectApi,
    Libraries.connectAvro,
    Libraries.connectJson,
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeParser,
    Libraries.sttpCore,
    Libraries.sttpFutureBackend
  )

  val test = Seq(
    Libraries.scalaTest        % Test,
    Libraries.mockitoCore      % Test,
    Libraries.mockitoScalatest % Test
  )
}
