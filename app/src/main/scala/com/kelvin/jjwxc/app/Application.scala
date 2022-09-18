package com.kelvin.jjwxc.app

import io.circe.generic.auto._
import cats.implicits._
import cats.effect.{ExitCode, IO, Resource}
import com.kelvin.jjwxc.auth.{AuthTokenService, JjwxcToken}
import com.kelvin.jjwxc.chrome.SearchPageChromeRequestService
import com.kelvin.jjwxc.config.ApplicationConfig
import com.kelvin.jjwxc.orchestration.{ChromeWithToken, OrchestrationService}
import com.kelvin.jjwxc.search.SearchUrlService
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.chrome._
import org.typelevel.log4cats.Logger
import io.circe.fs2._
import fs2._
import fs2.io.file.{Files, Path}
import org.joda.time.DateTime
import org.openqa.selenium.Cookie

import java.util
import scala.jdk.CollectionConverters._
object Application {
  def run(searchTerms: List[(String, String)], config: ApplicationConfig)(implicit logger: Logger[IO]): IO[ExitCode] = {

    val chromeWithTokenResource = for {
      _ <- Resource.eval(logger.info("Initialising Chrome Driver"))
      _ <- Resource.eval(IO(WebDriverManager.chromedriver.setup()))

      token        <- loadOrCreateToken(config.jjwxcConfig.loginUrl)
      chromeDriver <- initialiseChromeDriver(config)

      _               <- Resource.eval(injectTokenToChrome(chromeDriver, token))
      chromeWithToken = ChromeWithToken(token = token, driver = chromeDriver)

      searchUrlService           <- SearchUrlService.resource(config.jjwxcConfig.hostUrl, config.jjwxcConfig.urlEncoding)
      searchChromeRequestService <- SearchPageChromeRequestService(chromeDriver)
      orchestrationService <- OrchestrationService.resource(
                               searchUrlService,
                               chromeWithToken,
                               searchChromeRequestService
                             )
    } yield orchestrationService

    chromeWithTokenResource
      .use { chromeWithToken =>
        chromeWithToken
          .orchestrateJjwxc(searchTerms)
          .compile
          .drain
          .map(_ => ExitCode.Success)
      }
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

  private def initialiseChromeDriver(
      config: ApplicationConfig
  )(implicit logger: Logger[IO]): Resource[IO, ChromeDriver] = {
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

      _ <- Resource.eval(IO(driver.get(config.jjwxcConfig.hostUrl)))
    } yield driver
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
