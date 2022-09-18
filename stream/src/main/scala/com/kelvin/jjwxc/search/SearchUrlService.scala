package com.kelvin.jjwxc.search
import cats.effect.{IO, Resource}
import fs2._

import java.net.{URL, URLEncoder}

class SearchUrlService(hostUrl: String, encoding: String) {
  def urlStream: Pipe[IO, SearchTerm, URL] = _.map { searchTerm =>
    val urlEncodeSearchTerm = urlEncode(searchTerm.searchTerm)
    val board               = searchTerm.section

    new URL(s"${hostUrl}search.php?act=search&board=$board&keyword=$urlEncodeSearchTerm&topicaccurate=1")
  }

  def urlEncode(searchTerm: String): String = URLEncoder.encode(searchTerm, encoding)
}

object SearchUrlService {
  def resource(hostUrl: String, encoding: String): Resource[IO, SearchUrlService] = {
    val acquire = IO(new SearchUrlService(hostUrl, encoding))
    Resource.eval(acquire)
  }
}
