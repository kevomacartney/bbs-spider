package com.kelvin.jjwxc.orchestration

import cats.effect.IO
import com.kelvin.jjwxc.data.IndexedPost
import io.circe.fs2._
import io.circe.generic.auto._
import fs2._
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.Logger
import spoiwo.model._
import spoiwo.model.enums.CellFill
import spoiwo.natures.xlsx.Model2XlsxConversions.XlsxWorkbook

import java.io.File

case class ExcelEntry(post: Row, comments: List[Row])

class SinkStream(cachePath: String, outputPath: String)(implicit val logger: Logger[IO]) {
  def stream: Stream[IO, Unit] = {
    val fileName    = "posts.xlsx"
    val cachedFiles = getCachedFileNames

    Stream.eval {
      prepareWrite(cachedFiles, fileName)
        .flatMap(rows => writeToFile(fileName, outputPath, rows))
    }
  }

  private def prepareWrite(cachedFiles: Array[String], fileName: String): IO[List[ExcelEntry]] = {
    Stream
      .emits(cachedFiles)
      .map(Path(_))
      .through(parseFile)
      .through(toRow)
      .compile
      .toList
  }

  private def writeToFile(fileName: String, outputPath: String, rows: List[ExcelEntry]) = {
    for {
      rowsWithHeader <- IO(withHeaderRow(rows))
      postSheet      = Sheet("Posts").addRows(rowsWithHeader.map(_.post))
      commentSheet   = Sheet("Comments").addRows(rowsWithHeader.flatMap(_.comments))
      workBook       = Workbook(postSheet, commentSheet)
      outputFilePath <- createOutFile(outputPath, fileName)
      _              <- IO(workBook.saveAsXlsx(outputFilePath))
    } yield ()
  }

  private def createOutFile(outputFilePath: String, fileName: String): IO[String] = {
    val fullOutputPath = s"$outputFilePath/$fileName"
    val outputFile     = new File(fullOutputPath)

    if (!outputFile.exists()) {
      for {
        _ <- IO(new File(outputFilePath).mkdir())
        _ <- logger.info(s"Created path $outputFilePath")
      } yield fullOutputPath
    } else {
      IO(outputFile.delete()) *> IO(fullOutputPath)
    }
  }

  private def withHeaderRow(entries: List[ExcelEntry]): List[ExcelEntry] = {
    val headerStyle =
      CellStyle(fillPattern = CellFill.Solid, fillForegroundColor = Color.AquaMarine, font = Font(bold = true))

    val postHeader = Row(style = headerStyle).withCellValues(
      "id",
      "Post Subject",
      "Category",
      "Url",
      "Original Post",
      "NO. Of Replies"
    )

    val commentHeader = Row(style = headerStyle).withCellValues(
      "ID",
      "Comment",
      "Time",
      "Owner"
    )

    val headerEntry = ExcelEntry(postHeader, List(commentHeader))
    List(headerEntry) ++ entries
  }

  private def getCachedFileNames = {
    new File(s"$cachePath")
      .listFiles(_.getName.contains(".json"))
      .map(_.getAbsolutePath)
  }
  private def parseFile: Pipe[IO, Path, IndexedPost] = _.flatMap { path =>
    Files[IO]
      .readAll(path)
      .through(byteStreamParser)
      .through(decoder[IO, IndexedPost])
  }

  private def toRow: Pipe[IO, IndexedPost, ExcelEntry] = _.map { post =>
    val postRow = Row().withCellValues(
      post.id.toString,
      post.postSubject,
      post.category,
      post.postUrl,
      post.originalPost,
      post.numberOfReplies
    )

    val commentRows = createCommentRows(post)

    ExcelEntry(postRow, commentRows)
  }

  private def createCommentRows(indexedPost: IndexedPost): List[Row] = {
    indexedPost.comments.map { comment =>
      Row().withCellValues(
        indexedPost.id.toString,
        comment.comment.replace("=", "'="),
        comment.commentTime,
        comment.commentOwner.replace("=", "'=")
      )
    }
  }
}
