ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.kirill"
ThisBuild / organizationName := "example"
ThisBuild / resolvers ++= Seq(
  "Confluent" at "https://packages.confluent.io/maven/",
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("public"),
  Resolver.sbtPluginRepo("releases")
)

releaseVersionBump := sbtrelease.Version.Bump.Next
releaseCrossBuild := false

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
    libraryDependencies ++= Dependencies.sink ++ Dependencies.test,
    assembly / assemblyJarName := "kafka-connect-http-sink.jar",
    assembly / assemblyOption := (assembly / assemblyOption).value.copy(
      includeScala = false,
      includeDependency = true
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  )
