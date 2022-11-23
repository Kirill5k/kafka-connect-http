import sbt.Keys.crossScalaVersions

val scala212               = "2.12.17"
val scala213               = "2.13.10"
val supportedScalaVersions = List(scala212, scala213)

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / organization := "io.github.kirill5k"
ThisBuild / organizationName := "kirill5k"
ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sbtPluginRepo("releases"),
  "Confluent" at "https://packages.confluent.io/maven/",
)

releaseVersionBump := sbtrelease.Version.Bump.Next
releaseCrossBuild := false

val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publish / skip := true
)

val commonSettings = Seq(
  organizationName := "Kafka Connect Http",
  startYear := Some(2020),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalafmtOnCompile := true,
  crossScalaVersions := supportedScalaVersions
)

val sink = project.in(file("connectors/sink"))
  .settings(commonSettings)
  .enablePlugins(BuildInfoPlugin)
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
    Compile / assembly / artifact := {
      val art = (Compile / assembly / artifact).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(Compile / assembly / artifact, assembly)
  )
  .enablePlugins(AutomateHeaderPlugin)

val root = project.in(file("."))
  .settings(noPublish)
  .settings(
    name := "kafka-connect-http",
    crossScalaVersions := Nil
  )
  .aggregate(sink)