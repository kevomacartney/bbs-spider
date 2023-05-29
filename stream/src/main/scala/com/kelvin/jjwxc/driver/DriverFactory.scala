package com.kelvin.jjwxc.driver

import cats.effect.{IO, Resource}
import cats.implicits._
import com.kelvin.jjwxc.auth.{AuthTokenService, JjwxcToken}
import com.kelvin.jjwxc.orchestration.DriverWithToken
import io.circe.fs2._
import io.circe.generic.auto._
import io.github.bonigarcia.wdm.WebDriverManager
import fs2._
import fs2.io.file.{Files, Path}
import org.joda.time.DateTime
import org.openqa.selenium.Cookie
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.remote.RemoteWebDriver
import org.typelevel.log4cats.Logger

import java.util
import java.io.File
import scala.jdk.CollectionConverters._

object DriverFactory {
  def newDriver(hostUrl: String, loginUrl: String, workingDirectory: String)(
      implicit logger: Logger[IO]
  ): Resource[IO, DriverWithToken] = {
    for {
      _ <- Resource.eval(logger.info("Initialising Driver"))
      _ <- Resource.eval(IO(WebDriverManager.firefoxdriver().setup()))

      token  <- loadOrCreateToken(loginUrl, workingDirectory)
      driver <- initialiseDriverDriver()

      _               <- Resource.eval(IO(driver.get(hostUrl)))
      _               <- Resource.eval(injectTokenToChrome(driver, token))
      driverWithToken = DriverWithToken(token = token, driver = driver)

    } yield driverWithToken
  }

  def createChromeOptions: ChromeOptions = {
    val options: util.List[String] = List(
      //      "--headless",
      "--disable-gpu",
      "--window-size=1920,1200",
      "--ignore-certificate-errors",
      "--disable-extensions",
      "--no-sandbox",
      "--disable-dev-shm-usage",
      "--remote-allow-origins=*"
    ).asJava

    new ChromeOptions().addArguments(options)
  }

  private def createFirefoxOptions: FirefoxOptions = {
    val options: util.List[String] = List(
      "--window-size=1920,1200"
    ).asJava

    new FirefoxOptions().addArguments(options)
  }

  private def initialiseDriverDriver()(implicit logger: Logger[IO]): Resource[IO, RemoteWebDriver] = {
    for {
      _      <- Resource.eval(logger.info("Starting Driver"))
      driver <- Resource.make(IO(new FirefoxDriver(createFirefoxOptions)))((d: RemoteWebDriver) => IO(d.close()))
//      driver <- Resource.make(IO(new ChromeDriver(createOptions)))((driver: RemoteWebDriver) => IO(driver.close()))
    } yield driver
  }

  private def loadOrCreateToken(loginUrl: String, workingDirectory: String)(
      implicit logger: Logger[IO]
  ): Resource[IO, JjwxcToken] = {
    loadAuthToken(workingDirectory).flatMap { loadedToken =>
      loadedToken
        .fold(AuthTokenService.createAuthToken(loginUrl, workingDirectory)) { token =>
          for {
            _ <- Resource.eval(logger.info(s"Loaded token from disk: $token") *> IO(token))
          } yield token
        }
    }
  }

  private def loadAuthToken(workingDirectory: String)(implicit logger: Logger[IO]): Resource[IO, Option[JjwxcToken]] = {
    val tokenPath = s"$workingDirectory/token.json"

    if (new File(tokenPath).exists()) {
      Files[IO]
        .readAll(Path(tokenPath))
        .through(text.utf8.decode)
        .through(stringStreamParser)
        .through(decoder[IO, JjwxcToken])
        .compile
        .resource
        .last
    } else {
      for {
        _ <- Resource.eval(logger.info(s"Could not find auth token at [$tokenPath]"))
      } yield None
    }
  }

  private def injectTokenToChrome(chromeDriver: RemoteWebDriver, token: JjwxcToken): IO[Unit] = {
    def createCookie(cookieName: String) = {
      val domain = "bbs.jjwxc.net"
      val expiry = DateTime.now().plusMonths(12).toDate
      new Cookie(cookieName, token.token, domain, "/", expiry, true, false)
    }

    List("bbstoken", "token")
      .map(createCookie)
      .map(cookie => IO(chromeDriver.manage().addCookie(cookie)))
      .sequence_
  }
}
