import sbt.*
import sbt.Keys.resolvers
import Dependencies.*
import Resolvers.*

lazy val assemblySettings = Seq(
  assembleArtifact := true,
  assembly / assemblyMergeStrategy := {
    case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
    case x if x.endsWith("BUILD")                        => MergeStrategy.first
    case x if x.contains("module-info.class")            => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val commonSettings = Seq(
  resolvers := defaultResolvers,
  libraryDependencies ++= List(
    Cats.cats,
    Cats.`cats-effect`,
    Cats.`cats-effect-concurrent`,
    Cats.`cats-retry`,
    Logging.`log4cats`,
    Logging.`log4cats-slf4j`,
    Logging.`log4j-core`,
    Logging.logBack,
    Misc.`scala-url-builder`,
    Misc.`scala-swing-ui`,
    Misc.`joda-time`,
    Fs2.fs2,
    Misc.`spowio-excel`
  ),
  ThisBuild / scalaVersion := "2.13.8",
  dependencyOverrides ++= Dependencies.overrides,
  publish := {},
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
  //  allows for graceful shutdown of containers once the tests have finished running.
  Test / fork := true
)

lazy val `jjwxc` = (project in file("."))
  .settings(assemblySettings)
  .settings(
    ThisBuild / organization := "Kelvin Macartney",
    ThisBuild / version := "0.1",
    assembly / assemblyJarName := "jjwxc-spider.jar",
    assembly / mainClass := Some("com.kelvin.jjwxc.app.Main")
  )
  .aggregate(`app`, `stream`)
  .dependsOn(`app`, `stream`)

lazy val `app` = (project in file("./app"))
  .dependsOn(`stream`)
  .settings(commonSettings)
  .settings(assemblySettings)
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
  .settings(assemblySettings)
  .settings(`fs2-File`)
  .settings(selenium)
  .settings(
    resolvers := defaultResolvers,
    name := "app",
    Test / fork := true,
    libraryDependencies ++= List(
      Misc.`nscala-time`,
      Circe.`circe-parser`,
      Circe.`circe-generic`,
      Circe.`circe-fs2`
    )
  )
