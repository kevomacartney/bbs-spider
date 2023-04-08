package com.kelvin.jjwxc.post

import cats.implicits._
import com.kelvin.jjwxc.data.{Comment, IndexedPost}
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.{By, WebElement}

import java.net.URL
import java.util.UUID
import scala.jdk.CollectionConverters._

object PostParserService {
  def processPost(driver: RemoteWebDriver, postUrl: URL): IndexedPost = {
    val category     = getCategory(driver)
    val topicSubject = getPostSubject(driver)
    val originalPost = getOriginalPost(driver)
    val comments     = getTopicComments(driver)

    IndexedPost(
      id = UUID.randomUUID(),
      category = category,
      postSubject = topicSubject,
      postUrl = postUrl.toString,
      originalPost = originalPost,
      comments = comments,
      numberOfReplies = comments.size
    )
  }

  private def getPostSubject(driver: RemoteWebDriver): String = {
    driver.findElement(By.id("msgsubject")).getText
  }

  private def getOriginalPost(driver: RemoteWebDriver): String =
    driver.findElement(By.id("topic")).getText

  private def getTopicComments(driver: RemoteWebDriver): List[Comment] = {
    val userAndDate = getUserAndDate(driver)
    val comments = driver
      .findElements(By.className("read"))
      .asScala
      .toList
      .drop(1) // drop original post
      .map(_.getText)

    comments
      .padZipWith(userAndDate) { (maybeComment, maybeUserAndDate) =>
        (maybeComment, maybeUserAndDate).mapN { (comment, userAndDate) =>
          Comment(
            comment = comment,
            commentTime = userAndDate._2,
            commentOwner = userAndDate._1
          )
        }
      }
      .flatten
  }

  private def getCategory(driver: RemoteWebDriver): String = {
    driver.findElement(By.id("boardname")).getText.replace("\n", " ")
  }

  private def getUserAndDate(driver: RemoteWebDriver): List[(String, String)] = {
    driver
      .findElements(By.className("authorname"))
      .asScala
      .drop(1) // drop original post
      .map(extractUserAndDate)
      .toList
  }

  private def extractUserAndDate(element: WebElement): (String, String) = {
    val taggedElements = element
      .findElements(By.xpath(".//*"))
      .asScala
      .filter(it => it.getTagName == "font" || it.getTagName == "span")
      .map(_.getText)

    val text = element.getText

    val userAndDate = taggedElements
      .fold(text)((currentText, taggedElem) => currentText.replace(taggedElem, ""))
      .replace("留言", "")
      .replace("\n", "")
      .split('|')
      .map(_.strip())

    (userAndDate.head, userAndDate.last)
  }
}
