package com.kelvin.jjwxc.chrome
import cats.effect.{IO, Resource}
import fs2._
import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.chrome.ChromeDriver

import scala.jdk.CollectionConverters._
import java.net.URL

class SearchPageChromeRequestService(chromeDriver: ChromeDriver) {
  def searchPageChromeRequestStream: Pipe[IO, URL, WebElement] = _.flatMap { uri =>
    sendRequest(uri)
    clickSubmit()

    val elements = getElements
    Stream.emits(elements)
  }

  private def sendRequest(uri: URL): Unit =
    chromeDriver.get(uri.toString)

  private def clickSubmit(): Unit = {
    chromeDriver
      .findElements(By.tagName("input"))
      .asScala
      .toList
      .filter(e => e.getAttribute("value") == "查询" || e.getAttribute("value") == "inquire")
      .head
      .click()
  }

  private def getElements: List[WebElement] = {
    def extractElements: List[WebElement] = {
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
    }

    def isNextPageButton(element: WebElement): Boolean = {
      val option = for {
        _      <- Option(element.getText)
        option <- Option(element.getAttribute("href")).map(_.isEmpty)
      } yield option

      option.getOrElse(false)
    }

    def hasNextPage: Option[WebElement] = {
      val q = chromeDriver
        .findElements(By.tagName("a"))
        .asScala
        .toList
        .map(element => Option.when(element.getText != null)(element))
        .filter {
          case Some(value) => isNextPageButton(value)
          case None        => false
        }
        .head

      println(q)
      q
    }

    val firstPage = extractElements

    while (hasNextPage.isDefined) {
      hasNextPage.get.click()
      extractElements
    }
    firstPage
  }
}

object SearchPageChromeRequestService {
  def apply(chromeDriver: ChromeDriver): Resource[IO, SearchPageChromeRequestService] = {
    Resource.eval(IO(new SearchPageChromeRequestService(chromeDriver)))
  }
}
