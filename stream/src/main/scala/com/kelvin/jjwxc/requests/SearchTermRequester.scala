package com.kelvin.jjwxc.requests

import cats.effect._
import com.kelvin.jjwxc.model.GeneratedUrlContext
import fs2._
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.Select
import org.typelevel.log4cats.Logger

import java.net.URL
import scala.jdk.CollectionConverters._

class SearchTermRequester(driver: RemoteWebDriver)(implicit logger: Logger[IO]) {
  def streamPosts: Pipe[IO, GeneratedUrlContext, URL] =
    _.evalMap(handleSearchRequest)
      .map(_ => driver)
      .through(SearchPageTopicStream.searchPageTopicStream)

  private def handleSearchRequest(context: GeneratedUrlContext): IO[Unit] = {
    for {
      _ <- populateSearchInputs(context.searchUrl)
      _ <- setSearchTopic(context.topic)

      _ <- submitSearchRequest()
    } yield ()
  }

  private def populateSearchInputs(uri: URL): IO[Unit] =
    IO(driver.get(uri.toString))

  private def setSearchTopic(topic: String): IO[Unit] = {
    IO {
      new Select(driver.findElement(By.id("topic"))).selectByValue(topic)
    }.onError { err =>
      logger.error(err)(s"Could not find topic '$topic' using default topic.")
    }
  }

  private def submitSearchRequest(): IO[Unit] = {
    IO {
      driver
        .findElements(By.tagName("input"))
        .asScala
        .toList
        .filter(e => e.getAttribute("value") == "查询" || e.getAttribute("value") == "inquire")
        .head
        .click()
    }
  }
}

object SearchTermRequester {
  def apply(driver: RemoteWebDriver)(implicit logger: Logger[IO]): Resource[IO, SearchTermRequester] = {
    Resource.eval(IO(new SearchTermRequester(driver)))
  }
}
