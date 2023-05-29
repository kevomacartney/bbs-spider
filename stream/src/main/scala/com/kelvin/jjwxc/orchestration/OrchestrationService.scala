package com.kelvin.jjwxc.orchestration

import cats.effect.{IO, Resource}
import com.kelvin.jjwxc.Commands
import com.kelvin.jjwxc.cache.{IndexedPostCachingStream, UrlHash}
import com.kelvin.jjwxc.data.IndexedPost
import com.kelvin.jjwxc.model.SearchTerm
import com.kelvin.jjwxc.post.PostParserService
import com.kelvin.jjwxc.requests.SearchTermRequester
import com.kelvin.jjwxc.search.SearchUrlGenererator
import fs2._
import org.typelevel.log4cats.Logger
import com.kelvin.jjwxc.Compile
import java.net.URL

class OrchestrationService(
    searchUrlGenerator: SearchUrlGenererator,
    searchTermRequester: SearchTermRequester,
    chromePool: DriverInstancePool,
    indexedPostCacheStream: IndexedPostCachingStream,
    cachedPostsHashes: List[String],
    sink: SinkStream,
    parallelization: Int,
    command: Option[Commands]
)(
    implicit logger: Logger[IO]
) {
  def orchestrateJjwxc(searchTerms: List[SearchTerm]): Stream[IO, Unit] = {
    val baseStream = command match {
      case Some(Compile) => Stream.empty
      case _             => Stream.emits(searchTerms)
    }

    val crawlStream = baseStream
      .through(searchUrlGenerator.stream)
      .through(searchTermRequester.streamPosts)
      .through(dropCached)
      .parEvalMapUnordered(parallelization)(indexPage)
      .collect {
        case Some(post) => post
      }
      .through(indexedPostCacheStream.stream)
      .drain

    crawlStream ++ sink.stream
  }

  private def dropCached: Pipe[IO, URL, URL] = _.evalFilterNotAsync(parallelization) { url =>
    for {
      hashedUrl <- IO(UrlHash.hash(url.toString))
      result    <- IO(cachedPostsHashes.contains(hashedUrl))
    } yield result
  }

  private def indexPage(url: URL): IO[Option[IndexedPost]] = {
    chromePool.requestCompute { driver =>
      for {
        _           <- logger.info(s"Indexing post: $url")
        _           <- IO(driver.get(url.toString)) *> logger.info("Running get page IO")
        indexedPage <- IO(PostParserService.processPost(driver, url))
      } yield indexedPage
    }
  }
}

object OrchestrationService {
  def resource(
      searchUrlService: SearchUrlGenererator,
      searchRequesterService: SearchTermRequester,
      chromePool: DriverInstancePool,
      indexedPostCacheStream: IndexedPostCachingStream,
      cachedPostsHashes: List[String],
      sink: SinkStream,
      parallelization: Int,
      command: Option[Commands]
  )(
      implicit logger: Logger[IO]
  ): Resource[IO, OrchestrationService] = {
    Resource.eval(
      IO {
        new OrchestrationService(
          searchUrlService,
          searchRequesterService,
          chromePool,
          indexedPostCacheStream,
          cachedPostsHashes,
          sink,
          parallelization,
          command
        )
      }
    )
  }
}
