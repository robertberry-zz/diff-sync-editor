package model

import play.api.libs.json._

object SocketInMessage {
  implicit val jsonReads = new Reads[SocketInMessage] {
    override def reads(json: JsValue): JsResult[SocketInMessage] =
      json \ "type" match {
        case JsString("update") =>
          Json.fromJson[UpdateCommand](json \ "mergeDiffs")

        case JsString("refresh") =>
          JsSuccess(RefreshCommand)

        case other =>
          JsError(s"Bad type field: $other")
      }
  }
}

sealed trait SocketInMessage

object SocketOutMessage {
  implicit val jsonWrites = new Writes[SocketOutMessage] {
    override def writes(o: SocketOutMessage): JsValue = o match {
      case UpdateCommand(merge) =>
        JsObject(Seq(
          "type" -> JsString("update"),
          "mergeDiffs" -> Json.toJson(merge)
        ))
    }
  }
}

sealed trait SocketOutMessage

object UpdateCommand {
  implicit val jsonReads = Json.reads[UpdateCommand]
}

case class UpdateCommand(command: MergeDiffs) extends SocketInMessage with SocketOutMessage

case object RefreshCommand extends SocketInMessage

object ResetDocument {
  implicit val jsonWrites = Json.writes[ResetDocument]
}

case class ResetDocument(document: Document) extends SocketOutMessage