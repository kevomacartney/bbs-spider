package com.kelvin.jjwxc.requests

import cats.effect.IO
import fs2._
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.Select

import java.net.URL
import scala.jdk.CollectionConverters._

object SearchPageTopicStream {
  def searchPageTopicStream: Pipe[IO, RemoteWebDriver, URL] = _.flatMap { driver =>
    getAllSearchTermTopics(driver)
  }

  private def getAllSearchTermTopics(driver: RemoteWebDriver): Stream[IO, URL] = {
    def navigateToPage(newPage: Int): Unit = {
      pageSelector(driver).selectByIndex(newPage)
    }

    val totalPageCount = getResultsPageCount(driver)

    Stream.range(1, totalPageCount).flatMap { nextPage =>
      val urls = extractCurrentPageTopicLinks(driver)
      navigateToPage(nextPage)
      Stream.emits(urls)
    }
  }

  private def pageSelector(driver: RemoteWebDriver): Select = {
    new Select(driver.findElement(By.id("selectpage")))
  }

  private def extractCurrentPageTopicLinks(driver: RemoteWebDriver): List[URL] = {
    driver
      .findElements(By.tagName("a"))
      .asScala
      .toList
      .filter { element =>
        Option(element.getAttribute("href")) match {
          case Some(value) => value.contains("showmsg")
          case None        => false
        }
      }
      .map(_.getAttribute("href"))
      .map(new URL(_))
  }

  private def getResultsPageCount(driver: RemoteWebDriver): Int = {
    pageSelector(driver).getOptions.asScala.last
      .getAttribute("value")
      .toInt
  }
}
