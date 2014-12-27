package model

object Document {
  val empty = Document("")
}

case class Document(
  body: String
)