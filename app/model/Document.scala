package model

import play.api.libs.json.Json

object Document {
  implicit val jsonWrites = Json.writes[Document]

  val empty = Document("")
}

case class Document(
  body: String
)