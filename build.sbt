import sbt._
import sbt.Keys.resolvers
import Dependencies._
import Resolvers._

lazy val `jjwxc` = (project in file("."))
  .settings(
    ThisBuild / scalaVersion := "2.13.8",
    ThisBuild / organization := "Kelvin Macartney",
    ThisBuild / version := "0.1"
  )
  .aggregate(`app`)

lazy val commonSettings = Seq(
  resolvers := defaultResolvers,
  libraryDependencies ++= List(
    Cats.cats,
    Cats.`cats-effect`,
    Logging.`log4cats`,
    Logging.`log4cats-slf4j`,
    Logging.logBack,
    Misc.`scala-url-builder`,
    Misc.`scala-swing-ui`,
    Fs2.fs2
  ),
  dependencyOverrides ++= Dependencies.overrides,
  publish := {},
  //  allows for graceful shutdown of containers once the tests have finished running.
  Test / fork := true
)

lazy val `app` = (project in file("./app"))
  .dependsOn(`stream`)
  .settings(commonSettings)
  .settings(
    resolvers := defaultResolvers,
    name := "app",
    Test / fork := true,
    libraryDependencies ++= List(
      Circe.`circe-parser`,
      Config.`pure-config`,
      Browser.`selenium-java`,
      Browser.webdrivermanager,
      Fs2.`fs2-io`,
      Circe.`circe-fs2`,
      Circe.`circe-parser`,
      Circe.`circe-generic`
    )
  )

lazy val `stream` = (project in file("./stream"))
  .settings(commonSettings)
  .settings(
    resolvers := defaultResolvers,
    name := "app",
    Test / fork := true,
    libraryDependencies ++= List(
      Browser.`selenium-java`,
      Misc.`nscala-time`
    )
  )
