package com.kelvin.jjwxc.orchestration

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.kelvin.jjwxc.chrome.SearchPageChromeRequestService
import com.kelvin.jjwxc.search.SearchTermStream.searchTermsStream
import com.kelvin.jjwxc.search.SearchUrlService
import fs2._
import org.openqa.selenium.{WebDriver, WebElement}
import org.typelevel.log4cats.Logger

class OrchestrationService(
    searchUrlService: SearchUrlService,
    chromeWithToken: ChromeWithToken,
    searchChromeRequestService: SearchPageChromeRequestService
)(
    implicit logger: Logger[IO]
) {
  def orchestrateJjwxc(searchTerms: List[(String, String)]): Stream[IO, WebElement] = {
    val cores = Runtime.getRuntime.availableProcessors
    Stream
      .emits(searchTerms)
      .through(searchTermsStream)
      .through(searchUrlService.urlStream)
      .through(searchChromeRequestService.searchPageChromeRequestStream)
      .map { a =>
        logger.info(a.getText).unsafeRunSync()
        a
      }
  }
}

object OrchestrationService {
  def resource(
      searchUrlService: SearchUrlService,
      chromeWithToken: ChromeWithToken,
      chromeRequestService: SearchPageChromeRequestService
  )(
      implicit logger: Logger[IO]
  ): Resource[IO, OrchestrationService] = {
    Resource.eval(IO(new OrchestrationService(searchUrlService, chromeWithToken, chromeRequestService)))
  }
}
