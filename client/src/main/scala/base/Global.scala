package base

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import shared.model.{User, AppConfig}
import shared.basic.AppError
import shared.MainIds.*
import base.Logging.*
import comps.Sidebar


@js.native
@JSGlobal("Math")
object Math extends js.Object {
  def random(): Double = js.native
}

object UUIDGen {
  def generate: String = {
    val randomPart = Math.random().toString.substring(2, 15) + System.currentTimeMillis() % 10000
    randomPart
  }
}


trait Mgmt extends JsWrapper:
  def validUser = Global.user != None
  def getUser = Global.user

  def ucError(err: AppError): Unit =
    pages.Auth.hide()

    removeClass(gE(ContentId), "d-none")
    if pages.PgError.render(err) then
      Sidebar.setNavLink("Error")
    else
      error(s"exec -> usecase:Error ${err}")

enum AuthMode:
  case Nonce
  case AppPassword

/**
 * Globaler Zustand des Clients: Benutzer, Sprache, URLs und Konfiguration aus WordPress.
 */
object Global:
  val localStoragePrefix = "App."
  /** Präfix der Schlüssel in session-/localStorage. */
  val StoragePrefix = "webapp_"
  var user : Option[User] = None
  var lang    = ""
  var version = ""

  /** App-Konfiguration (Menü, Karten, statische Seiten) aus config/app.json des Plugins. */
  var appConfig: AppConfig = AppConfig()

  var csrf    = ""
  var homeUrl = ""
  var dataUrl = ""
  var imgUrl  = ""
  var playUrl = ""
  /** True, wenn die einbettende WordPress-Seite die Homepage ist. */
  var isHomePage: Boolean = false
  /** localStorage-Schlüssel der gewählten Sprache (gesetzt beim Umschalten oder beim Besuch einer /en/-URL). */
  val LangStorageKey = StoragePrefix + "lang"
  /** True, wenn englischsprachigen Besuchern einer deutschen Seite der Hinweis auf die englische Fassung angezeigt wird. */
  var showLangHint: Boolean = false
  /** True, wenn der Benutzer Schreibrechte auf die einbettende Seite hat (von WordPress ermittelt). */
  var canEditPage: Boolean = false
  var activePageName: String = ""
  var pageId  = 0
  var hostPageId = 0
  var wpNonce   = ""
  var turnstileSitekey = ""
  /** Social-Media-Links für den Footer (aus .env über data-Attribute; leer = Button ausgeblendet). */
  var socialFacebook  = ""
  var socialX         = ""
  var socialInstagram = ""

  var authMode: AuthMode = AuthMode.Nonce
  var wpUserName: String = ""
  var wpAppPassword: String = ""

  /** True, wenn ein Administrator angemeldet ist. */
  def isAdmin: Boolean = user.exists(_.roles.contains("administrator"))

  /**
   * Prüft die Sichtbarkeit eines Menüeintrags.
   *
   * @param visible all, guest (nicht angemeldet), user (angemeldet) oder admin
   * @return True, wenn der Eintrag für den aktuellen Benutzer sichtbar ist
   */
  def isVisible(visible: String): Boolean = visible match
    case "guest" => user.isEmpty
    case "user"  => user.isDefined
    case "admin" => isAdmin
    case _       => true

  /** Entfernt den gespeicherten Seitenzustand aus dem sessionStorage. */
  def clearPageState(): Unit =
    try
      val storage = dom.window.sessionStorage
      Seq("last_page", "last_param", "last_wp_page_id").foreach(k => storage.removeItem(StoragePrefix + k))
    catch
      case _: Exception => // Storage nicht verfügbar

  def setUser(usr: User) = user = Some(usr)
  def resetUser: Unit = {
    user = None
    try {
      val adminBar = dom.document.getElementById("wpadminbar")
      if (adminBar != null) {
        adminBar.remove()
      }
      val body = dom.document.body
      if (body != null) {
        body.classList.remove("admin-bar")
        body.classList.remove("logged-in")
        body.style.marginTop = ""
      }
      val html = dom.document.documentElement.asInstanceOf[dom.raw.HTMLElement]
      if (html != null) {
        html.classList.remove("html-admin-bar")
        html.style.marginTop = ""
      }
    } catch {
      case _: Exception => // ignore
    }
    clearPageState()
  }
