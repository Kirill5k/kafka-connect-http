import sbt.Keys.crossScalaVersions

lazy val scala212               = "2.12.10"
lazy val scala213               = "2.13.5"
lazy val supportedScalaVersions = List(scala212, scala213)

ThisBuild / credentials += Credentials(
  "emnify/emnify-maven",
  "emnify-648956897802.d.codeartifact.eu-west-1.amazonaws.com",
  "aws",
  sys.env.getOrElse("CODEARTIFACT_AUTH_TOKEN", "Unknown")
)

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / organization := "io.github.kirill5k"
ThisBuild / organizationName := "example"
ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sbtPluginRepo("releases"),
  "Confluent" at "https://packages.confluent.io/maven/",
)

releaseVersionBump := sbtrelease.Version.Bump.Next
releaseCrossBuild := false

lazy val root = (project in file("."))
  .settings(
    name := "kafka-connect-http",
    crossScalaVersions := Nil
  )
  .aggregate(sink)

lazy val commonSettings = Seq(
  organizationName := "Kafka Connect Http",
  startYear := Some(2020),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalafmtOnCompile := true,
  crossScalaVersions := supportedScalaVersions
)

lazy val sink = (project in file("connectors/sink"))
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
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  )
  .enablePlugins(AutomateHeaderPlugin)
