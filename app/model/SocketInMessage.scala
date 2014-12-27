package model

import play.api.libs.json._

sealed trait SocketInMessage

sealed trait SocketOutMessage

object UpdateCommand {
  // Je suis lazy
  implicit val jsonReads = Json.reads[UpdateCommand]

  implicit val jsonWrites = new Writes[UpdateCommand] {
    override def writes(o: UpdateCommand): JsValue = JsObject(Seq(
      "type" -> JsString("update"),
      "mergeDiffs" -> Json.toJson(o.mergeDiffs)
    ))
  }
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

case object RefreshCommand extends SocketInMessage

object ResetDocument {
  implicit val jsonWrites = new Writes[ResetDocument] {
    override def writes(o: ResetDocument): JsValue = JsObject(Seq(
      "type" -> JsString("reset"),
      "document" -> Json.toJson(o.document)
    ))
  }
}

case class ResetDocument(document: Document) extends SocketOutMessage