package actors

import java.util

import akka.actor.{Props, Actor}
import model.Document
import name.fraser.neil.plaintext.diff_match_patch.Diff
import scala.collection.JavaConverters._

object DocumentActor {
  case object GetDocument

  case class GetDocumentResponse(document: Document)
  case class UpdateDocument(edits: Seq[Diff])

  case class UpdateSuccess(document: Document)

  def props() = Props(classOf[DocumentActor])
}

class DocumentActor extends Actor {
  import DocumentActor._

  var document = Document.empty

  override def receive: Receive = {
    case GetDocument => sender ! GetDocumentResponse(document)

    case UpdateDocument(edits) =>
      val patches = DiffMatchPatch.patch_make(document.body, new util.LinkedList(edits.toList.asJava))
      document = Document(DiffMatchPatch.patch_apply(patches, document.body)(0).asInstanceOf[String])
      sender ! UpdateSuccess(document)
  }
}
