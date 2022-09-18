package com.kelvin.jjwxc.auth

import cats.effect.{IO, Resource}
import org.openqa.selenium.{Cookie, WebDriver}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.support.ui.WebDriverWait
import org.typelevel.log4cats.Logger

import java.time.Duration
import java.util
import scala.jdk.CollectionConverters._

object AuthTokenService {
  def createAuthToken(loginUrl: String)(implicit logger: Logger[IO]): Resource[IO, JjwxcToken] = {
    for {
      options <- makeChromeOptions
      driver  <- makeChromeDriver(options)
      _       = openAuthTokenWindow(driver, loginUrl)
      token   = retrieveToken(driver, loginUrl)
      _       <- Resource.eval(logger.info(s"Token acquired [${token.token}]"))
    } yield token
  }

  def makeChromeOptions: Resource[IO, ChromeOptions] = {
    val chromeArguments: util.List[String] = List(
      "--window-size=300,300",
      "--ignore-certificate-errors",
      "--disable-extensions",
      "--no-sandbox",
      "--disable-dev-shm-usage"
    ).asJava

    Resource.eval(IO(new ChromeOptions)).map(_.addArguments(chromeArguments))
  }

  def makeChromeDriver(chromeOptions: ChromeOptions): Resource[IO, ChromeDriver] = {
    val acquire = IO(new ChromeDriver(chromeOptions))
    val release = (driver: ChromeDriver) => IO(driver.quit())

    Resource.make(acquire)(release)
  }

  def openAuthTokenWindow(chromeDriver: ChromeDriver, loginUrl: String): Unit = {
    chromeDriver.get(loginUrl)
  }

  def retrieveToken(chromeDriver: ChromeDriver, loginUrl: String): JjwxcToken = {
    val wait = new WebDriverWait(chromeDriver, Duration.ofMinutes(5))
    wait.until { driver =>
      val hasTokenCookie = getCookie(driver, "token").isDefined
      hasTokenCookie
    }

    val cookieValue = getCookie(chromeDriver, "token").getOrElse(throw new Exception("Token cookie not in browser"))
    JjwxcToken(token = cookieValue.getValue)
  }

  def getCookie(chromeDriver: WebDriver, name: String): Option[Cookie] = {
    Option(chromeDriver.manage().getCookieNamed(name))
  }
}
