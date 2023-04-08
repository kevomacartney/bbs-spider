package com.kelvin.jjwxc.app

import cats.effect.{ExitCode, IO, IOApp}
import com.kelvin.jjwxc.config.ApplicationConfig
import com.kelvin.jjwxc.model.SearchTerm
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig._
import pureconfig.generic.auto._

import scala.util.matching.Regex

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      logger <- Slf4jLogger.create[IO]
      _      <- logger.info("Starting jjwxc spider")

      searchTerms <- parseSearchTerms(args)
      config      <- loadConfig
      exitCode    <- Application.run(searchTerms, config)(logger)

      _ <- logger.info(s"jjwxc indexing completed")
    } yield exitCode
  }

  def parseSearchTerms(searchTerms: List[String]): IO[List[SearchTerm]] = {
    val subsectionRegex  = "^([^:]+)".r
    val searchTermsRegex = ":([^:]+)$".r
    val searchTopic      = ":([^:]+):".r

    def extractTerm(regex: Regex, term: String) = {
      regex.findFirstMatchIn(term) match {
        case Some(value) => value.group(1)
        case None        => throw new Exception(s"'$term' is not a valid search topic")
      }
    }

    IO {
      searchTerms.flatMap { arg =>
        val subSection = extractTerm(subsectionRegex, arg)

        val searchTerms = extractTerm(searchTermsRegex, arg)
          .split(",")
          .map(_.trim)
          .filter(_.nonEmpty)

        val topic = extractTerm(searchTopic, arg)

        searchTerms.map(searchTerm => SearchTerm(section = subSection, searchTopic = topic, searchTerm = searchTerm))
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
