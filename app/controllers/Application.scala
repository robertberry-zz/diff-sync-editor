package controllers

import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsValue
import play.api.mvc._
import actors.{DocumentActor, ServerShadowActor}

object Application extends Controller {
  /** For now just a single document, but this could easily be changed to perform a look up for the appropriate
    * document.
    */
  val documentActor = Akka.system.actorOf(DocumentActor.props())

  def index = Action {
    Ok(views.html.index())
  }

  def socket = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    ServerShadowActor.props(out, documentActor)
  }
}