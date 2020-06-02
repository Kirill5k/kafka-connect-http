import sbt._

object Dependencies {
  object Versions {
    lazy val connectApi = "2.5.0"

    lazy val scalatest = "3.1.1"
  }

  object Libraries {
    lazy val connectApi = "org.apache.kafka" % "connect-api" % Versions.connectApi

    lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest
  }

  lazy val sink = Seq(
    Libraries.connectApi
  )

  lazy val test = Seq(
    Libraries.scalaTest % Test
  )
}
