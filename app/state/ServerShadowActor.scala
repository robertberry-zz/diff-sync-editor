package state

import akka.actor.{Stash, ActorRef, Actor}
import crypto.SHA1
import model.Document
import name.fraser.neil.plaintext.diff_match_patch.Diff
import state.ServerShadowActor.{ResetRequest, ResetDocument, MergeDiffs}

object ServerShadowActor {
  type Checksum = String

  case class MergeDiffs(diffs: Seq[Diff], shadowChecksum: Checksum)

  case class ResetDocument(document: Document)

  case object ResetRequest
}

class ServerShadowActor(socketActor: ActorRef, documentActor: ActorRef) extends Actor with Stash {
  documentActor ! DocumentActor.GetDocument

  override def receive: Receive = {
    case DocumentActor.GetDocumentResponse(document) =>
      context.become(withShadow(document))
  }

  def withShadow(shadow: Document): Receive = {
    case MergeDiffs(edits, checksum) =>
      if (SHA1.checksum(shadow.body) != checksum) {
        socketActor ! ResetDocument(shadow)
      } else {
        documentActor ! DocumentActor.UpdateDocument(edits)
        context.become(applyingEdits(shadow))
      }

    case ResetRequest => socketActor ! ResetDocument(shadow)
  }

  def applyingEdits(shadow: Document): Receive = {
    case DocumentActor.UpdateSuccess(newDocument) =>
      val diffs = DiffMatchPatch.diff_main(shadow.body, newDocument.body)
      socketActor ! MergeDiffs(Seq(diffs.toArray.asInstanceOf[Array[Diff]]: _*), SHA1.checksum(shadow.body))
      context.become(withShadow(newDocument))
      unstashAll()

    case _ => stash()
  }
}
