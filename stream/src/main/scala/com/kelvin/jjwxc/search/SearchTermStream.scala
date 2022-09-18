package com.kelvin.jjwxc.search
import cats.effect.IO
import fs2.{Pipe, Stream}

object SearchTermStream {

  def searchTermsStream: Pipe[IO, (String, String), SearchTerm] = _.map(tuple => SearchTerm(tuple._1, tuple._2))
}
