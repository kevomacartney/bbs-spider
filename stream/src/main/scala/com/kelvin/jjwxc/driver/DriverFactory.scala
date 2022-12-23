package com.kelvin.jjwxc.driver

import cats.effect.{IO, Resource}
import cats.implicits._
import com.kelvin.jjwxc.auth.{AuthTokenService, JjwxcToken}
import com.kelvin.jjwxc.orchestration.ChromeWithToken
import io.circe.fs2._
import io.circe.generic.auto._
import io.github.bonigarcia.wdm.WebDriverManager
import fs2._
import fs2.io.file.{Files, Path}
import org.joda.time.DateTime
import org.openqa.selenium.Cookie
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.typelevel.log4cats.Logger

import java.util
import scala.jdk.CollectionConverters._

object DriverFactory {
  def newDriver(hostUrl: String, loginUrl: String)(implicit logger: Logger[IO]): Resource[IO, ChromeWithToken] = {
    for {
      _ <- Resource.eval(logger.info("Initialising Chrome Driver"))
      _ <- Resource.eval(IO(WebDriverManager.chromedriver.setup()))

      token        <- loadOrCreateToken(loginUrl)
      chromeDriver <- initialiseChromeDriver()

      _               <- Resource.eval(IO(chromeDriver.get(hostUrl)))
      _               <- Resource.eval(injectTokenToChrome(chromeDriver, token))
      chromeWithToken = ChromeWithToken(token = token, driver = chromeDriver)

    } yield chromeWithToken
  }

  private def initialiseChromeDriver()(implicit logger: Logger[IO]): Resource[IO, ChromeDriver] = {
    val chromeArguments: util.List[String] = List(
      //      "--headless",
      "--disable-gpu",
      "--window-size=1920,1200",
      "--ignore-certificate-errors",
      "--disable-extensions",
      "--no-sandbox",
      "--disable-dev-shm-usage"
    ).asJava

    for {
      options <- Resource.eval(IO(new ChromeOptions))
      _       = options.addArguments(chromeArguments)

      makeDriver    = IO(new ChromeDriver(options))
      releaseDriver = (d: ChromeDriver) => IO(d.close())
      driver        <- Resource.make(makeDriver)(releaseDriver)

    } yield driver
  }

  private def loadOrCreateToken(loginUrl: String)(implicit logger: Logger[IO]): Resource[IO, JjwxcToken] = {
    loadAuthToken.flatMap { loadedToken =>
      loadedToken
        .fold(AuthTokenService.createAuthToken(loginUrl)) { token =>
          val io = logger.info(s"Loaded token from disk: $token") *> IO(token)
          Resource.eval(io)
        }
    }
  }

  private def loadAuthToken(implicit logger: Logger[IO]): Resource[IO, Option[JjwxcToken]] = {
    val localTokenPath = getClass.getResource("/token.json").getPath

    Files[IO]
      .readAll(Path(localTokenPath))
      .through(text.utf8.decode)
      .through(stringStreamParser)
      .through(decoder[IO, JjwxcToken])
      .compile
      .resource
      .last
  }

  private def injectTokenToChrome(chromeDriver: ChromeDriver, token: JjwxcToken): IO[Unit] = {
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
