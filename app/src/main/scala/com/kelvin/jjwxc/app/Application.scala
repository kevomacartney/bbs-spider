package com.kelvin.jjwxc.app

import cats.effect.{ExitCode, IO, Resource}
import com.kelvin.jjwxc.{Commands, Compile}
import com.kelvin.jjwxc.cache.IndexedPostCachingStream
import com.kelvin.jjwxc.config.ApplicationConfig
import com.kelvin.jjwxc.driver.DriverFactory
import com.kelvin.jjwxc.model.SearchTerm
import com.kelvin.jjwxc.orchestration.{DriverInstancePool, OrchestrationService, SinkStream}
import com.kelvin.jjwxc.requests.SearchTermRequester
import com.kelvin.jjwxc.search.SearchUrlGenererator
import org.typelevel.log4cats.Logger

import java.io.File

object Application {
  def run(searchTerms: List[SearchTerm], config: ApplicationConfig, command: String)(
      implicit logger: Logger[IO]
  ): IO[ExitCode] = {

    val chromeWithTokenResource = for {
      _ <- Resource.eval(logger.info("Initialising Driver"))

      workingDir = jarDirectory()
      _          <- Resource.eval(logger.info(s"Working directory [$workingDir]"))
      cacheDir   = s"$workingDir/cache"
      sinkDir    = s"$workingDir/sink"
      loginUrl   = config.jjwxcConfig.loginUrl
      hostUrl    = config.jjwxcConfig.hostUrl

      driverWithToken            <- DriverFactory.newDriver(hostUrl, loginUrl, workingDir)
      searchUrlService           <- SearchUrlGenererator.resource(config.jjwxcConfig.hostUrl, config.jjwxcConfig.urlEncoding)
      searchChromeRequestService <- SearchTermRequester(driverWithToken.driver)
      indexedPostCacheStream     <- Resource.Eval(IO(IndexedPostCachingStream(cacheDir)))
      threadCount                = 1 //Runtime.getRuntime.availableProcessors
      chromePool                 <- DriverInstancePool(threadCount, hostUrl, workingDir)
      cachedPosts                <- Resource.eval(IO(loadCachedPosts(cacheDir)))
      sink                       <- Resource.eval(IO(new SinkStream(cachePath = cacheDir, outputPath = sinkDir)))

      orchestrationService <- OrchestrationService.resource(
                               searchUrlService,
                               searchChromeRequestService,
                               chromePool,
                               indexedPostCacheStream,
                               cachedPosts,
                               sink,
                               threadCount,
                               toCommands(command)
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

  private def loadCachedPosts(cachePath: String): List[String] = {
    val cacheDir = new File(s"$cachePath")

    if (cacheDir.exists()) {
      cacheDir.listFiles(_.isFile).map(_.getName.replace(".json", "")).toList
    } else {
      List()
    }
  }

  private def jarDirectory(): String = {
    val codeSource = getClass.getProtectionDomain.getCodeSource
    val jarFile    = new File(codeSource.getLocation.toURI.getPath)

    jarFile.getParentFile.getPath
  }

  private def toCommands(command: String): Option[Commands] = {
    command.toLowerCase() match {
      case "compile" => Some(Compile)
      case _         => None
    }
  }
}
