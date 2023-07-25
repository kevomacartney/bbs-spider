import sbt.Keys.libraryDependencies
import sbt.*

object Dependencies {
  lazy val overrides: Seq[Nothing] = Seq(
    )
  object Browser {
    var `selenium-java`  = "org.seleniumhq.selenium" % "selenium-java"    % "4.9.1"
    var webdrivermanager = "io.github.bonigarcia"    % "webdrivermanager" % "5.3.3"
  }

  object Fs2 {
    val version  = "3.2.12"
    val fs2      = "co.fs2" %% "fs2-core" % version
    val `fs2-io` = "co.fs2" %% "fs2-io" % version
  }

  object Circe {
    private lazy val circeVersion = "0.14.2"

    val `circe-parser`  = "io.circe" %% "circe-parser"  % circeVersion
    val `circe-generic` = "io.circe" %% "circe-generic" % circeVersion
    val `circe-fs2`     = "io.circe" %% "circe-fs2"     % "0.14.0"
  }
  object Cats {
    val cats                     = "org.typelevel"    %% "cats-core"              % "2.8.0"
    val `cats-effect`            = "org.typelevel"    %% "cats-effect"            % "3.3.14"
    val `cats-effect-concurrent` = "org.typelevel"    %% "cats-effect-concurrent" % "3.0-8096649"
    val `cats-retry`             = "com.github.cb372" %% "cats-retry"             % "3.1.0"

  }

  object Logging {
    val `log4cats`        = "org.typelevel"            %% "log4cats-slf4j" % "2.5.0"
    val `log4cats-slf4j`  = "org.typelevel"            %% "log4cats-slf4j" % "2.5.0"
    lazy val logBack      = "ch.qos.logback"           % "logback-classic" % "1.4.6"
    lazy val `log4j-core` = "org.apache.logging.log4j" % "log4j-core"      % "2.20.0"
  }

  object Misc {
    val `scala-url-builder` = "org.f100ded.scala-url-builder" %% "scala-url-builder" % "0.9.1"
    val `scala-swing-ui`    = "org.scala-lang.modules"        %% "scala-swing"       % "3.0.0"
    val `nscala-time`       = "com.github.nscala-time"        %% "nscala-time"       % "2.32.0"
    val `joda-time`         = "joda-time"                     % "joda-time"          % "2.12.2"
    val `spowio-excel`      = "com.norbitltd"                 %% "spoiwo"            % "2.2.1"
  }

  object Config {
    val `pure-config` = "com.github.pureconfig" %% "pureconfig" % "0.17.1"
  }

  object Resilience4j {
    val `resilience4j` = "io.github.resilience4j" % "resilience4j-all" % "2.0.2"
  }

  lazy val `fs2-File` = Seq(
    libraryDependencies ++= List(
      Circe.`circe-fs2`,
      Circe.`circe-parser`,
      Circe.`circe-generic`,
      Fs2.`fs2-io`
    )
  )

  lazy val selenium = Seq(
    libraryDependencies ++= List(
      Browser.`selenium-java`,
      Browser.webdrivermanager
    )
  )
}
