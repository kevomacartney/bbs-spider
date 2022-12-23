package com.kelvin.jjwxc.search
import cats.effect.IO
import fs2.{Pipe, Stream}

object SearchTermStream {
  def stream: Pipe[IO, (String, String), SearchTerm] = _.map(tuple => SearchTerm(section = tuple._1, searchTerm = tuple._2))
}
