package pages

import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.Event
import base.*
import shared.BoxButton
import shared.model.Text

/**
 * Generische Platzhalterseite für Menüeinträge und Karten ohne eigene Umsetzung.
 * Zeigt Titel, Icon und Beschreibung des Eintrags aus der App-Konfiguration sowie Beispieldaten.
 */
object Mockup extends BasePage with JsWrapper:
  def name = PageNameTyp("Mockup")
  val BtnAction: HtmlId = genId(name)
  val BtnBack:   HtmlId = genId(name)

  /**
   * Titel, Icon und Beschreibung zu einer ID aus Menü oder Karten.
   *
   * @param id ID des Menüeintrags bzw. der Karte
   * @return (Titel, Icon, Beschreibung)
   */
  def info(id: String): (Text, String, Text) =
    comps.Actions.menuItem(id).map(i => (i.label, i.icon, i.desc))
      .orElse(Global.appConfig.cards.find(_.id == id).map(c => (c.title, c.icon, c.desc)))
      .getOrElse((Text(id, id), "bi-app", Text()))

  /**
   * Rendert die Mockup-Seite.
   *
   * @param param ID des Menüeintrags bzw. der Karte
   * @return true
   */
  def render(param: String = ""): Boolean =
    val (title, icon, desc) = info(param)
    setMain(cviews.pages.html.Mockup(param, title.get(Global.lang), icon, desc.get(Global.lang)))
    true

  override def handleEvent(elem: HTMLElement, event: Event): Unit =
    HtmlId(elem.id) match
      case `BtnAction` => dialogs.DlgMsgbox.show(gM("mockup.notImplemented"), gM("mockup.badge"), List(BoxButton.Close))
      case `BtnBack`   => loadPage(MainView.name, "")
      case _           => debug(s"Mockup handleEvent: ${elem.id}")
