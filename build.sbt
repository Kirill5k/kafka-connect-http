ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.kirill"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "kafka-connect-http",
    publish / skip := true
  )
  .aggregate(sink)

lazy val sink = (project in file("modules/sink"))
  .settings(
    name := "kafka-connect-http-sink",
    moduleName := "kafka-connect-http-sink",
    libraryDependencies ++= Dependencies.sink
  )
