package com.kelvin.jjwxc.orchestration

import cats.effect._
import cats.effect.std.Queue
import cats.implicits._
import com.kelvin.jjwxc.driver.DriverFactory
import org.openqa.selenium.remote.RemoteWebDriver
import org.typelevel.log4cats.Logger
import retry.RetryDetails._
import retry._

class DriverInstancePool(queue: Queue[IO, RemoteWebDriver])(implicit logger: Logger[IO]) {
  def requestCompute[T](f: RemoteWebDriver => IO[T]): IO[Option[T]] = {
    for {
      driver <- queue.take
      result <- runWithRetry(f, driver)
      _      <- queue.offer(driver)
    } yield result
  }

  private def runWithRetry[T](f: RemoteWebDriver => IO[T], driver: RemoteWebDriver): IO[Option[T]] = {
    retryingOnAllErrors[T](
      policy = RetryPolicies.limitRetries[IO](5),
      onError = logError
    )(f(driver)).option
  }

  private def logError(err: Throwable, details: RetryDetails): IO[Unit] = {
    details match {
      case GivingUp(totalRetries, totalDelay) =>
        logger.error(err)(
          s"Giving up on request after $totalRetries retries and $totalDelay. [Error=${err.getMessage}]"
        )
      case WillDelayAndRetry(nextDelay, retriesSoFar, cumulativeDelay) =>
        logger.warn(err)(s"Failed to handle request. So far we have retried $retriesSoFar.")
    }
  }
}

object DriverInstancePool {
  def apply(instanceCount: Int, hostUrl: String, workingDir: String)(
      implicit logger: Logger[IO]
  ): Resource[IO, DriverInstancePool] = {
    for {
      _ <- Resource.eval(logger.info(s"Creating chrome $instanceCount workers"))

      queue   <- Resource.eval(Queue.bounded[IO, RemoteWebDriver](instanceCount))
      drivers <- createInstances(instanceCount, hostUrl, workingDir)

      _    <- Resource.eval(drivers.traverse(queue.offer))
      pool <- Resource.eval(IO(new DriverInstancePool(queue)))
    } yield pool
  }

  private def createInstances(instanceCount: Int, hostUrl: String, workingDir: String)(
      implicit logger: Logger[IO]
  ): Resource[IO, List[RemoteWebDriver]] = {
    List
      .fill(instanceCount)(())
      .traverse(_ => DriverFactory.newDriver(hostUrl, "", workingDir).map(_.driver))
  }
}
