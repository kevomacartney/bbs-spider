package com.kelvin.jjwxc.app

import cats.effect.{ExitCode, IO, Resource}
import com.kelvin.jjwxc.config.ApplicationConfig
import com.kelvin.jjwxc.driver.DriverFactory
import com.kelvin.jjwxc.orchestration.{ChromeInstancePool, OrchestrationService}
import com.kelvin.jjwxc.requests.SearchTermRequester
import com.kelvin.jjwxc.search.SearchUrlGenererator
import io.github.bonigarcia.wdm.WebDriverManager
import org.typelevel.log4cats.Logger

object Application {
  def run(searchTerms: List[(String, String)], config: ApplicationConfig)(implicit logger: Logger[IO]): IO[ExitCode] = {

    val chromeWithTokenResource = for {
      _ <- Resource.eval(logger.info("Initialising Chrome Driver"))
      _ <- Resource.eval(IO(WebDriverManager.chromedriver.setup()))

      loginUrl        = config.jjwxcConfig.loginUrl
      hostUrl         = config.jjwxcConfig.hostUrl
      chromeWithToken <- DriverFactory.newDriver(hostUrl, loginUrl)

      searchUrlService           <- SearchUrlGenererator.resource(config.jjwxcConfig.hostUrl, config.jjwxcConfig.urlEncoding)
      searchChromeRequestService <- SearchTermRequester(chromeWithToken.driver)
      threadCount                = 1 //Runtime.getRuntime.availableProcessors
      chromePool                 <- ChromeInstancePool(threadCount, hostUrl)
      orchestrationService <- OrchestrationService.resource(
                               searchUrlService,
                               searchChromeRequestService,
                               chromePool,
                               threadCount
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
}
