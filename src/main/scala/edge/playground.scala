package edge

import edge.json.BoxData

import scala.io.Source
import io.circe.parser.decode

object playground extends App {
  val jsonString = Source.fromFile("mempoolBoxData.json").mkString

  val parsedResult = decode[List[BoxData]](jsonString)

  parsedResult match {
    case Right(boxDataList) =>
      // Successfully parsed JSON data
      boxDataList.foreach(println)
    case Left(error) =>
      // Failed to parse JSON data
      println(s"Failed to parse JSON: $error")
  }
}
