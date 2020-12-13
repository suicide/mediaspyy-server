import com.typesafe.sbt.packager.docker.DockerVersion

val versions = new {
  val zio = "1.0.3"
  val zioInterop = "2.2.0.1"
  val zioLogging = "0.5.4"

  val http4s = "0.21.14"
  val logback = "1.2.3"

  val scalatest = "3.2.3"
  val scalatic = "3.2.2"

  val scala = "2.13.4"
}

val dependencies = {
  import versions._
  new {
    val zio = "dev.zio" %% "zio" % versions.zio
    val `zio-interop` = "dev.zio" %% "zio-interop-cats" % zioInterop
    val `zio-logging` = "dev.zio" %% "zio-logging" % zioLogging
    val `zio-logging-slf4j` = "dev.zio" %% "zio-logging-slf4j" % zioLogging

    val `http4s-blaze-server` = "org.http4s" %% "http4s-blaze-server" % http4s
    val `http4s-circe` = "org.http4s" %% "http4s-circe" % http4s
    val `http4s-dsl` = "org.http4s" %% "http4s-dsl" % http4s
    val `logback-classic` = "ch.qos.logback" % "logback-classic" % logback

    val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % "test"
    val scalatic = "org.scalactic" %% "scalactic" % versions.scalatic
  }
}

val commonSettings = Seq(
  organization := "rip.suiiii",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := versions.scala,
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  dependencyOverrides ++= {
    import dependencies._
    Seq(
      scalatest,
    )
  },
  dockerVersion := Some(DockerVersion(19, 3, 13, Some("ce"))),
  dockerBaseImage := "openjdk:11",
)

lazy val core = Project(
  id = "mediaspyy-core",
  base = file("core")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        `logback-classic`,
      )
    },
    unusedCompileDependenciesFilter -= moduleFilter("ch.qos.logback", "logback-classic"),
  )
  .settings(commonSettings: _*)
  .enablePlugins(ReproducibleBuildsPlugin)


lazy val app = Project(
  id = "mediaspyy-app",
  base = file("app")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        zio,
        `zio-interop`,
        `zio-logging`,
        `zio-logging-slf4j`,

        `http4s-blaze-server`,
        `http4s-circe`,
        `http4s-dsl`
      )
    },
    crossPaths := false,

    dockerExposedPorts ++= Seq(8080),
  )
  .settings(commonSettings: _*)
  .enablePlugins(ReproducibleBuildsPlugin, JavaAppPackaging, UniversalDeployPlugin, DockerPlugin)
  .dependsOn(core)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(core, app)
