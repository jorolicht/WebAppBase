package comps

import org.scalajs.dom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import base.*
import shared.MainIds.NavbarId
import shared.DomTypes.*
import shared.PageNameTyp.*
import comps.Sidebar.AsideId

/**
 * Navigationsleiste: Logo, Menü aus der App-Konfiguration (links), Sprachumschalter, Verwaltung und Benutzermenü (rechts).
 */
object Navbar extends BaseComp with base.JsWrapper with services.ComWrapper:
  def name = PageNameTyp("Navbar")
  val ToggleSidebarId:    HtmlId = genId(name)
  val ShowLoginId:        HtmlId = genId(name)
  val DoLogoutId:         HtmlId = genId(name)
  /** Präfix der HTML-IDs der Menüeinträge: Navbar_Menu-<id>. */
  val MenuPrefix = "Menu-"

  def render(param: String = "") =
    setHtml(gE(NavbarId), cviews.comps.html.navbar(Global.appConfig.menu))
    true

  override def handleEvent(elem: org.scalajs.dom.raw.HTMLElement, event: org.scalajs.dom.Event) =
    val key = elem.id.stripPrefix(s"${name.value}_")
    if key.startsWith(MenuPrefix) then
      Actions.menuItem(key.stripPrefix(MenuPrefix)).foreach(Actions.run)
    else HtmlId(elem.id) match
      case `ToggleSidebarId`    => toggleClass(gE(AsideId), "d-none")
      case `DoLogoutId`         => doLogout()
      case _                    => debug(s"event -> unknown event for elem:${elem.id} with event:${event.`type`}")

  /**
   * Meldet den Benutzer bei WordPress ab und zeigt die Abschiedsseite.
   */
  private def doLogout(): Unit =
    ajaxPost[String, Map[String, String]]("/wp-json/webapp/v1/auth/logout", List(), "", host = Global.homeUrl).map { res =>
      res match {
        case Right(m) => m.get("nonce").foreach(n => Global.wpNonce = n)
        case _ => // ignore
      }
      Global.resetUser
      render()
      pages.loadPage(shared.PageNameTyp("Goodbye"), "")
    }
