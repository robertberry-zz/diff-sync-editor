package actors

import akka.actor.{Props, Stash, ActorRef, Actor}
import crypto.SHA1
import grizzled.slf4j.Logging
import model._
import name.fraser.neil.plaintext.diff_match_patch.Diff
import actors.ServerShadowActor.ResetDocument
import play.api.libs.json.{Json, JsValue}

object ServerShadowActor {
  type Checksum = String

  case class ResetDocument(document: Document)

  def props(socketActor: ActorRef, documentActor: ActorRef) =
    Props(classOf[ServerShadowActor], socketActor, documentActor)
}

class ServerShadowActor(socketActor: ActorRef, documentActor: ActorRef) extends Actor with Stash with Logging {
  documentActor ! DocumentActor.GetDocument

  override def receive: Receive = {
    case DocumentActor.GetDocumentResponse(document) =>
      context.become(withShadow(document))
  }

  def withShadow(shadow: Document): Receive = {
    case json: JsValue =>
      Json.fromJson[SocketInMessage](json).asOpt match {
        case Some(UpdateCommand(MergeDiffs(edits, checksum))) =>
          if (SHA1.checksum(shadow.body) != checksum) {
            socketActor ! ResetDocument(shadow)
          } else {
            documentActor ! DocumentActor.UpdateDocument(edits)
            context.become(applyingEdits(shadow))
          }

        case Some(RefreshCommand) =>
          socketActor ! ResetDocument(shadow)

        case None =>
          logger.error(s"Bad JSON sent to server shadow: $json")
      }
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
