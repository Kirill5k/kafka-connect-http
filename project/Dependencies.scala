import sbt._

object Dependencies {

  object Versions {
    lazy val kafka     = "2.7.0"
    lazy val confluent = "6.1.0"
    lazy val circe     = "0.13.0"
    lazy val sttp      = "3.2.0"

    lazy val scalatest   = "3.2.6"
    lazy val mockito     = "1.16.32"
    lazy val mockserver  = "5.11.1"
    lazy val scalaCompat = "2.2.0"
  }

  object Libraries {

    lazy val scalaCompat = "org.scala-lang.modules"        %% "scala-collection-compat"      % Versions.scalaCompat
    lazy val connectApi  = "org.apache.kafka"               % "connect-api"                  % Versions.kafka
    lazy val connectAvro = "io.confluent"                   % "kafka-connect-avro-converter" % Versions.confluent
    lazy val connectJson = "org.apache.kafka"               % "connect-json"                 % Versions.kafka
    lazy val sttpCore    = "com.softwaremill.sttp.client3" %% "core"                         % Versions.sttp
    lazy val sttpCirce   = "com.softwaremill.sttp.client3" %% "circe"                        % Versions.sttp

    lazy val circeCore    = "io.circe" %% "circe-core"    % Versions.circe
    lazy val circeParser  = "io.circe" %% "circe-parser"  % Versions.circe
    lazy val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe

    lazy val scalaTest        = "org.scalatest"  %% "scalatest"               % Versions.scalatest
    lazy val mockitoCore      = "org.mockito"    %% "mockito-scala"           % Versions.mockito
    lazy val mockitoScalatest = "org.mockito"    %% "mockito-scala-scalatest" % Versions.mockito
    lazy val mockserver       = "org.mock-server" % "mockserver-netty"        % Versions.mockserver
  }

  lazy val sink = Seq(
    Libraries.scalaCompat,
    Libraries.connectApi,
    Libraries.connectAvro,
    Libraries.connectJson,
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeParser,
    Libraries.sttpCore,
    Libraries.sttpCirce
  )

  lazy val test = Seq(
    Libraries.scalaTest        % Test,
    Libraries.mockitoCore      % Test,
    Libraries.mockitoScalatest % Test,
    Libraries.mockserver       % Test
  )
}
