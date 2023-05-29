package com.kelvin.jjwxc.auth

import cats.effect.{IO, Resource}
import com.kelvin.jjwxc.driver.DriverFactory
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import fs2.io.file._
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.{Cookie, WebDriver}
import org.typelevel.log4cats.Logger

import java.time.Duration

object AuthTokenService {
  def createAuthToken(loginUrl: String, workingDir: String)(implicit logger: Logger[IO]): Resource[IO, JjwxcToken] = {
    for {
      driver <- makeDriver()
      _      = openAuthTokenWindow(driver, loginUrl)
      token  = retrieveToken(driver)
      _      <- Resource.eval(logger.info(s"Token acquired [${token.token}]"))
      _      <- writeToken(workingDir, token.token)
    } yield token
  }

  private def makeDriver(): Resource[IO, RemoteWebDriver] = {
    val acquire = IO(new ChromeDriver(DriverFactory.createChromeOptions))
    val release = (driver: RemoteWebDriver) => IO(driver.quit())

    Resource.make(acquire)(release)
  }

  private def openAuthTokenWindow(chromeDriver: RemoteWebDriver, loginUrl: String): Unit = {
    chromeDriver.get(loginUrl)
  }

  private def retrieveToken(chromeDriver: RemoteWebDriver): JjwxcToken = {
    val wait = new WebDriverWait(chromeDriver, Duration.ofMinutes(5))
    wait.until { driver =>
      val hasTokenCookie = getCookie(driver, "token").isDefined
      hasTokenCookie
    }

    val cookieValue = getCookie(chromeDriver, "token").getOrElse(throw new Exception("Token cookie not in browser"))
    JjwxcToken(token = cookieValue.getValue)
  }

  private def getCookie(chromeDriver: WebDriver, name: String): Option[Cookie] = {
    Option(chromeDriver.manage().getCookieNamed(name))
  }

  private def writeToken(workingDirectory: String, token: String): Resource[IO, Unit] = {
    val outputDir = Path(s"$workingDirectory/token.json")
    val tokeStr   = JjwxcToken(token).asJson.noSpaces
    Stream
      .eval(IO(tokeStr))
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(outputDir))
      .compile
      .resource
      .drain
  }
}
