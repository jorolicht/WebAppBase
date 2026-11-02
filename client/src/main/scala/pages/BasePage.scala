package pages

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement
import base.Logging.* 
import comps.Sidebar
import shared.basic.AppError
import shared.PageNameTyp.PageName

// pagesMap maps page names to page objects
val pagesMap = List(Auth, Console, PgError, Goodbye, RegistrationSuccess, MainView, Mockup,
                    UserRegistration, UserLogin, VerifyAccount, Management)
                    .map(pg => pg.name -> pg).toMap


/**
 * Lädt eine registrierte Seite in den Inhaltsbereich und merkt sie für die Wiederherstellung.
 *
 * @param pageName    Name der Seite (Schlüssel in pagesMap)
 * @param param       Seitenparameter
 * @param withSidebar Navigationslink in der Sidebar markieren
 * @param async       Seite über renderAsync laden
 */
def loadPage(pageName: PageName, param: String, withSidebar: Boolean = true, async: Boolean = false): Unit =
  try
    debug(s"loadPage -> pageName:${pageName} param:${param}")
    base.Global.activePageName = pageName.value
    def finish(success: Boolean): Unit =
      if success then
        if withSidebar then Sidebar.setNavLink(pageName.value)
        savePageState(pageName, param)
        syncUrl(pageName, param)
        comps.Navbar.render()
      else
        error(s"loadPage -> page:${pageName} param:${param}")
    if async then pagesMap(pageName).renderAsync(param).map(finish)
    else finish(pagesMap(pageName).render(param))
  catch
    case e: Exception => error(s"loadPage -> page:${pageName} param:${param} not found: ${e.getMessage}")

/**
 * Führt die Adresszeile für statische Seiten nach: /preise/ beim Öffnen der Seite,
 * zurück auf die Startseite beim Wechsel von einer statischen Seite zu einer anderen Seite.
 * Zurück/Vor im Browser behandelt der popstate-Listener in Main.
 *
 * @param pageName Geladene Seite.
 * @param param    Seitenparameter.
 */
private def syncUrl(pageName: PageName, param: String): Unit =
  val path = org.scalajs.dom.window.location.pathname
  val target =
    if (pageName == MainView.name && MainView.isStaticPage(param)) Some(MainView.staticUrl(param))
    else if (MainView.staticPageOfPath(path).isDefined) Some(MainView.staticUrl(""))
    else None
  target.filter(url => new org.scalajs.dom.URL(url).pathname != path).foreach { url =>
    org.scalajs.dom.window.history.pushState(null, "", url)
  }

private def savePageState(pageName: PageName, param: String): Unit =
  if (pageName.value == "Goodbye") return
  try
    val storage = org.scalajs.dom.window.sessionStorage
    val prefix = base.Global.StoragePrefix
    storage.setItem(prefix + "last_page", pageName.value)
    storage.setItem(prefix + "last_param", param)
    storage.setItem(prefix + "last_wp_page_id", base.Global.hostPageId.toString)
  catch
    case e: Exception => error(s"Failed to save page state to sessionStorage: ${e.getMessage}")


abstract class BasePage extends comps.BaseComp:
  override def handleEvent(elem: HTMLElement, event: Event): Unit = {}
