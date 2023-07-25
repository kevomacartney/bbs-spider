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

      searchTerms <- IO(parseSearchTerms(args))
      commands    <- IO(parseCommands(args))
      config      <- loadConfig
      exitCode    <- Application.run(searchTerms, config, commands)(logger)

      _ <- logger.info(s"jjwxc indexing completed")
    } yield exitCode
  }

  def parseSearchTerms(searchTerms: List[String]): List[SearchTerm] = {
    val subsectionRegex  = "^([^:]+)".r
    val searchTopic      = ":([^:]+):".r
    val searchTermsRegex = ":([^:]+)$".r

    def extractTerm(regex: Regex, term: String) = {
      regex.findFirstMatchIn(term) match {
        case Some(value) => value.group(1)
        case None        => throw new Exception(s"'$term' is not a valid command")
      }
    }

    searchTerms.take(1).flatMap { arg =>
      val subSection = extractTerm(subsectionRegex, arg)

      val searchTerms = extractTerm(searchTermsRegex, arg)
        .split(",")
        .map(_.trim)
        .filter(_.nonEmpty)

      val topic = extractTerm(searchTopic, arg)

      searchTerms.map(searchTerm => SearchTerm(section = subSection, searchTopic = topic, searchTerm = searchTerm))
    }
  }

  private def parseCommands(args: List[String]): Option[String] = {
    val commandsRegex = "--(\\w+)".r

    args.flatMap { arg =>
      commandsRegex.findFirstMatchIn(arg).map(_.group(1))
    }.headOption
  }

  def loadConfig: IO[ApplicationConfig] = {
    IO(
      ConfigSource.default
        .loadOrThrow[ApplicationConfig]
    )
  }
}
