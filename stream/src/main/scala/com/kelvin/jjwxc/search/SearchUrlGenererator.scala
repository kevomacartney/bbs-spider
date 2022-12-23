package com.kelvin.jjwxc.search
import cats.effect.{IO, Resource}
import fs2._

import java.net.{URL, URLEncoder}

class SearchUrlGenererator(hostUrl: String, encoding: String) {
  def stream: Pipe[IO, SearchTerm, URL] = _.map { searchTerm =>
    val urlEncodeSearchTerm = urlEncode(searchTerm.searchTerm)
    val board               = searchTerm.section

    new URL(s"${hostUrl}search.php?act=search&board=$board&keyword=$urlEncodeSearchTerm&topicaccurate=1")
  }

  def urlEncode(searchTerm: String): String = URLEncoder.encode(searchTerm, encoding)
}

object SearchUrlGenererator {
  def resource(hostUrl: String, encoding: String): Resource[IO, SearchUrlGenererator] = {
    val acquire = IO(new SearchUrlGenererator(hostUrl, encoding))
    Resource.eval(acquire)
  }
}
