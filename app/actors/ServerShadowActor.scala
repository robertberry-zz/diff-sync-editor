package actors

import akka.actor.{Props, Stash, ActorRef, Actor}
import crypto.SHA1
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
            context.become(applyingEdits(shadow))
          }

        case Some(RefreshCommand) =>
          socketActor ! Json.toJson(ResetDocument(shadow))

        case None =>
          Logger.error(s"Bad JSON sent to server shadow: $json")
      }
  }

  def applyingEdits(shadow: Document): Receive = {
    case DocumentActor.UpdateSuccess(newDocument) =>
      val diffs = DiffMatchPatch.diff_main(shadow.body, newDocument.body)
      socketActor ! Json.toJson(
        UpdateCommand(MergeDiffs(Seq(diffs.toArray.asInstanceOf[Array[Diff]]: _*), SHA1.checksum(shadow.body)))
      )
      context.become(withShadow(newDocument))
      unstashAll()

    case _ => stash()
  }
}
