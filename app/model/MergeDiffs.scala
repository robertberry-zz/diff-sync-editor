package model

import name.fraser.neil.plaintext.diff_match_patch.{Operation, Diff}
import play.api.libs.json._

object MergeDiffs {
  val opMap = Map(
    -1 -> Operation.DELETE,
    0 -> Operation.EQUAL,
    1 -> Operation.INSERT
  )

  val reverseOpMap = opMap.toSeq.map(_.swap).toMap

  implicit val diffJsonFormat = new Format[Diff] {
    override def reads(json: JsValue): JsResult[Diff] = json match {
      case JsArray(JsNumber(op @ -1 | 0 | 1), JsString(text)) =>
        JsSuccess(new Diff(opMap(op), text))
      case _ =>
        JsError("Diff must be array of operation and text")
    }

    override def writes(o: Diff): JsValue = JsArray(Seq(
      JsNumber(reverseOpMap(o.operation)),
      JsString(o.text)
    ))
  }

  implicit val jsonFormat = Json.format[MergeDiffs]
}

case class MergeDiffs(diffs: Seq[Diff], shadowChecksum: String)