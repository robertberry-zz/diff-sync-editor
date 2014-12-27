package model

import play.api.libs.json._

object SocketInMessage {
  implicit val jsonReads = new Reads[SocketInMessage] {
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

sealed trait SocketInMessage

object UpdateCommand {
  implicit val jsonReads = Json.reads[UpdateCommand]
}

case class UpdateCommand(command: MergeDiffs) extends SocketInMessage

case object RefreshCommand extends SocketInMessage
