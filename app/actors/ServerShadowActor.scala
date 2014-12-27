package actors

import akka.actor.{Props, Stash, ActorRef, Actor}
import crypto.SHA1
import lib.LinkedListHelper
import model._
import name.fraser.neil.plaintext.diff_match_patch.Diff
import play.api.Logger
import play.api.libs.json.{Json, JsValue}

object ServerShadowActor {
  def props(socketActor: ActorRef, documentActor: ActorRef) =
    Props(classOf[ServerShadowActor], socketActor, documentActor)
}

class ServerShadowActor(socketActor: ActorRef, documentActor: ActorRef) extends Actor with Stash {
  Logger.info("Creating server shadow actor ...")

  documentActor ! DocumentActor.GetDocument

  override def receive: Receive = {
    case DocumentActor.GetDocumentResponse(document) =>
      socketActor ! Json.toJson(ResetDocument(document))
      context.become(withShadow(document))
  }

  def withShadow(shadow: Document): Receive = {
    case json: JsValue =>
      Json.fromJson[SocketInMessage](json).asOpt match {
        case Some(UpdateCommand(MergeDiffs(edits, checksum))) =>
          val serverChecksum = SHA1.checksum(shadow.body)

          if (serverChecksum != checksum) {
            Logger.info(s"Client shadow checksum ($checksum) did not match server shadow checksum ($serverChecksum)")

            socketActor ! Json.toJson(ResetDocument(shadow))
          } else {
            documentActor ! DocumentActor.UpdateDocument(edits)
            context.become(applyingEdits(shadow, edits))
          }

        case Some(RefreshCommand) =>
          socketActor ! Json.toJson(ResetDocument(shadow))

        case None =>
          Logger.error(s"Bad JSON sent to server shadow: $json")
      }
  }

  def applyingEdits(shadow: Document, edits: Seq[Diff]): Receive = {
    case DocumentActor.UpdateSuccess(newDocument) =>
      val patch = DiffMatchPatch.patch_make(shadow.body, LinkedListHelper.fromTraversable(edits))
      val newShadow = Document(DiffMatchPatch.patch_apply(patch, shadow.body)(0).asInstanceOf[String])
      Logger.info(s"Patching server shadow (${shadow.body} -> ${newShadow.body})")

      val diffs = LinkedListHelper.toSeq(DiffMatchPatch.diff_main(newShadow.body, newDocument.body))
      socketActor ! Json.toJson(UpdateCommand(MergeDiffs(diffs, SHA1.checksum(newShadow.body))))
      context.become(withShadow(newDocument))
      unstashAll()

    case _ => stash()
  }
}
