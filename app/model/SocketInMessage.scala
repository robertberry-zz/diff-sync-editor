package model

import play.api.libs.json._

sealed trait SocketInMessage

sealed trait SocketOutMessage

object UpdateCommand {
  implicit val jsonFormat = Json.format[UpdateCommand]
}

case class UpdateCommand(mergeDiffs: MergeDiffs) extends SocketInMessage with SocketOutMessage

object SocketInMessage {
  implicit lazy val jsonReads = new Reads[SocketInMessage] {
    override def reads(json: JsValue): JsResult[SocketInMessage] =
      json \ "type" match {
        case JsString("update") =>
          Json.fromJson[UpdateCommand](json)

        case JsString("refresh") =>
          JsSuccess(RefreshCommand)

        case other =>
          JsError(s"Bad type field: $other")
      }
  }
}

object SocketOutMessage {
  implicit val jsonWrites = new Writes[SocketOutMessage] {
    override def writes(o: SocketOutMessage): JsValue = o match {
      case UpdateCommand(merge) =>
        JsObject(Seq(
          "type" -> JsString("update"),
          "mergeDiffs" -> Json.toJson(merge)
        ))

      case ResetDocument(document) =>
        JsObject(Seq(
          "type" -> JsString("reset"),
          "document" -> Json.toJson(document)
        ))
    }
  }
}

case object RefreshCommand extends SocketInMessage

object ResetDocument {
  implicit val jsonWrites = Json.writes[ResetDocument]
}

case class ResetDocument(document: Document) extends SocketOutMessage