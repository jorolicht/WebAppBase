package pages

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import base.*
import services.{ComWrapper, WebSocketService}

/**
 * Verwaltungsseite für Administratoren: Systeminformationen, Version des Play-Servers und WebSocket-Test.
 */
object Management extends BasePage with JsWrapper with ComWrapper:
  def name = PageNameTyp("Management")

  val ServerVersionId: HtmlId = genId(name)
  val WsChannelId:     HtmlId = genId(name)
  val WsMessageId:     HtmlId = genId(name)
  val WsLogId:         HtmlId = genId(name)
  val BtnWsConnect:    HtmlId = genId(name)
  val BtnWsSend:       HtmlId = genId(name)

  /**
   * Rendert die Seite (nur für Administratoren) und fragt die Version des Play-Servers ab.
   *
   * @param param nicht verwendet
   * @return false, wenn der Benutzer kein Administrator ist
   */
  def render(param: String = ""): Boolean =
    if (!Global.isAdmin) {
      setMain(cviews.pages.MainView.html.PageNotFound("Management", gM("management.accessDenied")))
      false
    } else {
      setMain(cviews.pages.html.Management(Global.user))
      loadServerVersion()
      true
    }

  /** Liest die Version des Play-Servers (/srv/helper/getMsg) und zeigt sie an. */
  private def loadServerVersion(): Unit =
    val base = if (Global.playUrl.nonEmpty) Global.playUrl.stripSuffix("/") else "/srv"
    dom.fetch(s"$base/helper/getMsg?msgCode=app.version").toFuture
      .flatMap(resp => resp.text().toFuture.map(t => if resp.ok then t else s"HTTP ${resp.status}"))
      .recover { case e => s"nicht erreichbar (${e.getMessage})" }
      .foreach(v => setHtml(gE(ServerVersionId), v))

  /** Hängt eine Zeile an das WebSocket-Protokoll an. */
  private def log(line: String): Unit =
    val elem = gE(WsLogId)
    if (elem != null) elem.textContent = elem.textContent + line + "\n"

  override def handleEvent(elem: HTMLElement, event: dom.Event): Unit =
    HtmlId(elem.id) match
      case `BtnWsConnect` =>
        val channel = getInput(gE(WsChannelId))
        WebSocketService.setListener(msg => log(s"<- $msg"))
        WebSocketService.connect(channel)
        log(s"verbinde mit Kanal '$channel' …")
      case `BtnWsSend` =>
        val msg = getInput(gE(WsMessageId))
        WebSocketService.send(msg)
        log(s"-> $msg")
      case _ => debug(s"Management handleEvent: ${elem.id}")
