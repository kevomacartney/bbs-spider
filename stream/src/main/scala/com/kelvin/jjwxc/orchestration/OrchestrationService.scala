package com.kelvin.jjwxc.orchestration

import cats.effect.{IO, Resource}
import com.kelvin.jjwxc.data.IndexedPost
import com.kelvin.jjwxc.post.PostParserService
import com.kelvin.jjwxc.requests.SearchTermRequester
import com.kelvin.jjwxc.search.{SearchTermStream, SearchUrlGenererator}
import fs2._
import org.typelevel.log4cats.Logger

import java.net.URL

class OrchestrationService(
    searchUrlGenerator: SearchUrlGenererator,
    searchTermRequester: SearchTermRequester,
    chromePool: ChromeInstancePool,
    parallelization: Int
)(
    implicit logger: Logger[IO]
) {
  def orchestrateJjwxc(searchTerms: List[(String, String)]): Stream[IO, IndexedPost] = {
    Stream
      .emits(searchTerms)
      .through(SearchTermStream.stream)
      .through(searchUrlGenerator.stream)
      .through(searchTermRequester.streamPosts)
      .parEvalMapUnordered(parallelization)(indexPage)
  }

  def indexPage(url: URL): IO[IndexedPost] = {
    chromePool.requestCompute { driver =>
      for {
        _           <- logger.info(s"Indexing post: $url")
        _           <- IO(driver.get(url.toString))
        indexedPage <- IO(PostParserService.processPost(driver))
      } yield indexedPage
    }
  }
}

object OrchestrationService {
  def resource(
      searchUrlService: SearchUrlGenererator,
      searchRequesterService: SearchTermRequester,
      chromePool: ChromeInstancePool,
      parallelization: Int
  )(
      implicit logger: Logger[IO]
  ): Resource[IO, OrchestrationService] = {
    Resource.eval(
      IO {
        new OrchestrationService(searchUrlService, searchRequesterService, chromePool, parallelization)
      }
    )
  }
}
