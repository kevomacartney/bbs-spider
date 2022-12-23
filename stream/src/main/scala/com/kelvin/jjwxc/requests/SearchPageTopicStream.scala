package com.kelvin.jjwxc.requests

import cats.effect.IO
import fs2._
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.Select

import java.net.URL
import scala.jdk.CollectionConverters._

object SearchPageTopicStream {
  def searchPageTopicStream: Pipe[IO, ChromeDriver, URL] = _.flatMap { chromeDriver =>
    getAllSearchTermTopics(chromeDriver)
  }

  private def getAllSearchTermTopics(chromeDriver: ChromeDriver): Stream[IO, URL] = {
    def navigateToPage(newPage: Int): Unit = {
      pageSelector(chromeDriver).selectByIndex(newPage)
    }

    val totalPageCount = getResultsPageCount(chromeDriver)

    Stream.range(1, totalPageCount).flatMap { nextPage =>
      val urls = extractCurrentPageTopicLinks(chromeDriver)
      navigateToPage(nextPage)
      Stream.emits(urls)
    }
  }

  private def pageSelector(chromeDriver: ChromeDriver): Select = {
    new Select(chromeDriver.findElement(By.id("selectpage")))
  }

  private def extractCurrentPageTopicLinks(chromeDriver: ChromeDriver): List[URL] = {
    chromeDriver
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

  private def getResultsPageCount(chromeDriver: ChromeDriver): Int = {
    pageSelector(chromeDriver).getOptions.asScala.last
      .getAttribute("value")
      .toInt
  }
}
