package com.kelvin.jjwxc.orchestration
import cats._
import cats.implicits._
import cats.effect._
import org.openqa.selenium.chrome.ChromeDriver
import cats.effect.std.{Queue, QueueSink, QueueSource}
import com.kelvin.jjwxc.driver.DriverFactory
import org.typelevel.log4cats.Logger
import fs2._

class ChromeInstancePool(queue: Queue[IO, ChromeDriver]) {
  def requestCompute[T](f: ChromeDriver => IO[T]): IO[T] = {
    for {
      driver <- queue.take
      result <- f(driver)
      _      <- queue.offer(driver)
    } yield result
  }
}

object ChromeInstancePool {
  def apply(instanceCount: Int, hostUrl: String)(implicit logger: Logger[IO]): Resource[IO, ChromeInstancePool] = {
    for {
      _ <- Resource.eval(logger.info(s"Creating chrome $instanceCount workers"))

      queue   <- Resource.eval(Queue.bounded[IO, ChromeDriver](instanceCount))
      drivers <- createInstances(instanceCount, hostUrl)

      _    <- Resource.eval(drivers.traverse(queue.offer))
      pool <- Resource.eval(IO(new ChromeInstancePool(queue)))
    } yield pool
  }

  private def createInstances(instanceCount: Int, hostUrl: String)(
      implicit logger: Logger[IO]
  ): Resource[IO, List[ChromeDriver]] = {
    List
      .fill(instanceCount)(())
      .traverse(_ => DriverFactory.newDriver(hostUrl, "").map(_.driver))
  }
}
