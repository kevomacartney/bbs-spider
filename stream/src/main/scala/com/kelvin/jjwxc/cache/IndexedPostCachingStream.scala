package com.kelvin.jjwxc.cache

import cats.effect.IO
import com.kelvin.jjwxc.data.IndexedPost
import fs2.io.file._
import fs2.{Pipe, Stream, text}
import io.circe.generic.auto._
import io.circe.syntax._

import java.io.File

class IndexedPostCachingStream(cachePath: String) {
  def stream: Pipe[IO, IndexedPost, IndexedPost] = _.flatMap { post =>
    val urlHash = UrlHash.hash(post.postUrl)
    val path    = s"$cachePath/$urlHash.json"
    Stream
      .emit[IO, IndexedPost](post)
      .map(_.asJson.noSpaces)
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(Path(path)))
      .map(_ => post)
  }
}

object IndexedPostCachingStream {
  def apply(cachePath: String): IndexedPostCachingStream = {
    createCacheDirectory(cachePath)
    new IndexedPostCachingStream(cachePath)
  }

  private def createCacheDirectory(cachePath: String): Unit = {
    new File(s"$cachePath").mkdirs()
  }
}
