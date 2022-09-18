package com.kelvin.jjwxc.app

import cats.effect.{ExitCode, IO, IOApp}
import com.kelvin.jjwxc.config.ApplicationConfig
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig._
import pureconfig.generic.auto._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      logger <- Slf4jLogger.create[IO]
      _      <- logger.info("Starting jjwxc spider")

      searchTerms <- parseSearchTerms(args)
      config      <- loadConfig
      exitCode    <- Application.run(searchTerms, config)(logger)

      _ <- logger.info(s"jjwxc indexing completed, saved to ${config.jjwxcConfig.saveDirectory}")
    } yield exitCode
  }

  def parseSearchTerms(searchTerms: List[String]): IO[List[(String, String)]] = {
    val subsectionRegex  = ".*(?=:)".r
    val searchTermsRegex = "[^:]+$".r

    IO {
      searchTerms.flatMap { arg =>
        val subSection = subsectionRegex
          .findFirstMatchIn(arg)
          .getOrElse(throw new Exception(s"'$arg' is not a valid search criteria"))
          .toString()

        val searchTerms = searchTermsRegex
          .findFirstMatchIn(arg)
          .getOrElse(throw new Exception(s"'$arg' is not a valid search criteria"))
          .toString()
          .split(",")
          .toList

        searchTerms.map(searchTerm => subSection -> searchTerm)
      }
    }
  }

  def loadConfig: IO[ApplicationConfig] = {
    IO(
      ConfigSource.default
        .loadOrThrow[ApplicationConfig]
    )
  }
}
