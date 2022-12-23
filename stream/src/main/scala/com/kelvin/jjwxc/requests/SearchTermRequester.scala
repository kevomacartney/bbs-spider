package com.kelvin.jjwxc.requests
import cats.syntax._
import cats.effect._
import fs2._
import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.Select
import org.typelevel.log4cats.Logger

import scala.jdk.CollectionConverters._
import java.net.URL
import scala.runtime.LazyLong

class SearchTermRequester(chromeDriver: ChromeDriver)(implicit logger: Logger[IO]) {
  def streamPosts: Pipe[IO, URL, URL] =
    _.map(handleSearchRequest)
      .map(_ => chromeDriver)
      .through(SearchPageTopicStream.searchPageTopicStream)

  private def handleSearchRequest(url: URL): Unit = {
    populateSearchInputs(url)
    submitSearchRequest()
  }

  private def populateSearchInputs(uri: URL): Unit =
    chromeDriver.get(uri.toString)

  private def submitSearchRequest(): Unit = {
    chromeDriver
      .findElements(By.tagName("input"))
      .asScala
      .toList
      .filter(e => e.getAttribute("value") == "查询" || e.getAttribute("value") == "inquire")
      .head
      .click()
  }
}

object SearchTermRequester {
  def apply(chromeDriver: ChromeDriver)(implicit logger: Logger[IO]): Resource[IO, SearchTermRequester] = {
    Resource.eval(IO(new SearchTermRequester(chromeDriver)))
  }
}
