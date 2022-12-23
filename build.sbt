import sbt._
import sbt.Keys.resolvers
import Dependencies._
import Resolvers._

lazy val `jjwxc` = (project in file("."))
  .settings(
    ThisBuild / organization := "Kelvin Macartney",
    ThisBuild / version := "0.1"
  )
  .aggregate(`app`)

lazy val commonSettings = Seq(
  resolvers := defaultResolvers,
  libraryDependencies ++= List(
    Cats.cats,
    Cats.`cats-effect`,
    Cats.`cats-effect-concurrent`,
    Logging.`log4cats`,
    Logging.`log4cats-slf4j`,
    Logging.logBack,
    Misc.`scala-url-builder`,
    Misc.`scala-swing-ui`,
    Misc.`joda-time`,
    Fs2.fs2
  ),
  ThisBuild / scalaVersion := "2.13.8",
  dependencyOverrides ++= Dependencies.overrides,
  publish := {},
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
  //  allows for graceful shutdown of containers once the tests have finished running.
  Test / fork := true
)

lazy val `app` = (project in file("./app"))
  .dependsOn(`stream`)
  .settings(commonSettings)
  .settings(`fs2-File`)
  .settings(selenium)
  .settings(
    resolvers := defaultResolvers,
    name := "app",
    Test / fork := true,
    libraryDependencies ++= List(
      Config.`pure-config`
    )
  )

lazy val `stream` = (project in file("./stream"))
  .settings(commonSettings)
  .settings(`fs2-File`)
  .settings(selenium)
  .settings(
    resolvers := defaultResolvers,
    name := "app",
    Test / fork := true,
    libraryDependencies ++= List(
      Misc.`nscala-time`
    )
  )
