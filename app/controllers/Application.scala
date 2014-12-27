package controllers

import crypto.SHA1
import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsString, JsObject, JsValue}
import play.api.mvc._
import actors.{DocumentActor, ServerShadowActor}
import Play.current

object Application extends Controller {
  Logger.info("Creating document actor")

  /** For now just a single document, but this could easily be changed to perform a look up for the appropriate
    * document.
    */
  val documentActor = Akka.system.actorOf(DocumentActor.props())

  def index = Action {
    Ok(views.html.index())
  }

  /** For debugging */
  def checksum = Action { implicit request =>
    request.getQueryString("q") match {
      case Some(q) =>
        Ok(JsObject(Seq(
          "checksum" -> JsString(SHA1.checksum(q))
        )))

      case None =>
        BadRequest("q?")
    }
  }

  def socket = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    ServerShadowActor.props(out, documentActor)
  }
}