package com.kelvin.jjwxc.search
import cats.effect.{IO, Resource}
import com.kelvin.jjwxc.Commands
import com.kelvin.jjwxc.model.{GeneratedUrlContext, SearchTerm}
import fs2._

import java.net.{URL, URLEncoder}

class SearchUrlGenererator(hostUrl: String, encoding: String) {
  def stream: Pipe[IO, SearchTerm, GeneratedUrlContext] = _.map { searchTerm =>
    val urlEncodeSearchTerm = urlEncode(searchTerm.searchTerm)
    val board               = searchTerm.section

    val url = new URL(s"${hostUrl}search.php?act=search&board=$board&keyword=$urlEncodeSearchTerm&topicaccurate=1")
    GeneratedUrlContext(url, searchTerm.searchTopic)
  }

  def urlEncode(searchTerm: String): String = URLEncoder.encode(searchTerm, encoding)
}

object SearchUrlGenererator {
  def resource(hostUrl: String, encoding: String): Resource[IO, SearchUrlGenererator] = {
    val acquire = IO(new SearchUrlGenererator(hostUrl, encoding))
    Resource.eval(acquire)
  }
}
