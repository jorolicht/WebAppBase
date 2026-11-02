import shared.basic.Pickle.*
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.*

import services.ComWrapper
import base.{ Global, JsWrapper, _ }
import comps.{ BaseComp, Navbar, Footer }

import shared.basic.*
import shared.MainIds.*
import shared.model.AppConfig

object Main extends BaseComp with ComWrapper with JsWrapper with Mgmt:
  def name = PageNameTyp("Main")

  /**
   * Startet die Anwendung (über den WordPress-Shortcode [webapp]).
   * URLs, Homepage und Schreibrechte werden von WordPress als data-Attribute übergeben.
   *
   * @param version   Versionskennung der Applikation (SemVer MAJOR.MINOR.PATCH)
   * @param logLevel  Log-Level ("debug", "info", "warn", "error")
   */
  @JSExportTopLevel("startApp")
  def startApp(version: String, logLevel: String): Unit =
    val effectiveVersion = if (version != null && version.nonEmpty) version else shared.AppVersion.version

    Global.dataUrl  = getData(gE(ParamId), "dataurl", "./data/")
    Global.imgUrl   = getData(gE(ParamId), "imgurl", "./img/")
    Global.playUrl  = getData(gE(ParamId), "playurl","")
    Global.homeUrl  = getData(gE(ParamId), "homeurl", "")
    Global.wpNonce  = getData(gE(ParamId), "nonce", "")
    Global.pageId   = getData(gE(ParamId), "pageid", 0)
    Global.hostPageId = Global.pageId
    Global.turnstileSitekey = getData(gE(ParamId), "turnstilesitekey", "")
    Global.socialFacebook   = getData(gE(ParamId), "socialfacebook", "")
    Global.socialX          = getData(gE(ParamId), "socialx", "")
    Global.socialInstagram  = getData(gE(ParamId), "socialinstagram", "")
    Global.isHomePage  = getData(gE(ParamId), "ishome", 0) == 1
    Global.canEditPage = getData(gE(ParamId), "canedit", 0) == 1
    val configUrl = getData(gE(ParamId), "configurl", "./config/app.json")
    // Sprache: aus der URL (data-lang, /en/… bzw. deutsche Seite), sonst die gemerkte Wahl, sonst die Browsersprache
    val pageLang   = getData(gE(ParamId), "lang", "")
    val storedLang = try Option(dom.window.localStorage.getItem(Global.LangStorageKey)).getOrElse("") catch { case _: Throwable => "" }
    val browserLang = dom.window.navigator.language.take(2)
    Global.lang = if (pageLang.nonEmpty) pageLang else if (storedLang.nonEmpty) storedLang else browserLang
    if (pageLang == "en") try dom.window.localStorage.setItem(Global.LangStorageKey, "en") catch { case _: Throwable => () }
    Global.showLangHint = pageLang == "de" && storedLang != "de" && browserLang != "de"
    Logging.setLogLevel(logLevel.toLowerCase())

    val startPage = getData(gE(ParamId), "page", "").toLowerCase()

    println(s"startApp -> dataUrl:${Global.dataUrl} version:${effectiveVersion} lang:${Global.lang} home:${Global.isHomePage} canEdit:${Global.canEditPage} page:${startPage} PageId:${Global.pageId} logLevel:${logLevel}")

    dom.window.asInstanceOf[js.Dynamic].appLoadPage = (pageName: String, param: String) => appLoadPage(pageName, param)
    dom.window.asInstanceOf[js.Dynamic].appEvent    = (elem: HTMLElement, event: dom.Event) => appEvent(elem, event)
    dom.window.asInstanceOf[js.Dynamic].appSwitchLang = (lang: String) => pages.MainView.switchLang(lang)
    // expose addon Console.start function, only if addon is included, otherwise dummy function
    dom.window.asInstanceOf[js.Dynamic].startConsole = () => addon.Console.start()

    // Zurück/Vor im Browser zwischen statischen Seiten (/preise/ …) und Startseite (URLs setzt pages.syncUrl)
    dom.window.addEventListener("popstate", (_: dom.PopStateEvent) => {
      val path = dom.window.location.pathname
      pages.MainView.staticPageOfPath(path) match
        case Some(page) => pages.loadPage(pages.MainView.name, page)
        case None if pages.MainView.isHomePath(path) => pages.loadPage(pages.MainView.name, "")
        case None => dom.window.location.reload()
    })

    for
      msgOk  <- Messages.initMsg(effectiveVersion, Global.dataUrl, Global.lang)
      config <- loadAppConfig(configUrl)
    do
      Global.appConfig = config
      if msgOk then initApp(startPage) else println("Main program failed to initialize")

  /**
   * Lädt die App-Konfiguration (Menü, Karten, statische Seiten); bei Fehlern leere Konfiguration.
   *
   * @param url URL von config/app.json im Plugin-Verzeichnis
   * @return Konfiguration
   */
  def loadAppConfig(url: String): Future[AppConfig] =
    dom.fetch(url).toFuture
      .flatMap(resp => if resp.ok then resp.text().toFuture else Future.failed(new Exception(s"HTTP ${resp.status}")))
      .map(text => read[AppConfig](text))
      .recover { case e: Throwable => error(s"loadAppConfig -> ${url}: ${e.getMessage}"); AppConfig() }

  /**
   * Lädt die Benutzerinformationen, rendert Navbar/Footer und stellt die letzte Seite wieder her
   * oder wählt die Startseite (siehe runDefaultRouting).
   *
   * @param startPage Optionale Startseite aus dem Shortcode-Attribut page (login, register, verify, static:<datei>)
   */
  def initApp(startPage: String): Unit =
    import shared.model.{UserInfo, User}

    ajaxGet[UserInfo]("/wp-json/webapp/v1/user", List(), Map("X-WP-NONCE"->Global.wpNonce), Global.homeUrl).map {
      case Right(ui) =>
        if (ui.user_id > 0) {
            Global.user = Some(User(id = ("", ui.user_id), username = ui.username, email = ui.email, firstname = ui.firstname,
              lastname = ui.lastname, org = ui.club, picUrl = ui.avatar_url, description = ui.description, roles = ui.roles,
              hasPasskey = ui.has_passkey
            ))
        }
        Navbar.render()
        Footer.render()

        // Letzte Seite der Sitzung wiederherstellen (nur auf der Homepage, nicht für statische Seiten)
        val storage = dom.window.sessionStorage
        val lastWpPageId = storage.getItem(Global.StoragePrefix + "last_wp_page_id")
        val lastPage     = storage.getItem(Global.StoragePrefix + "last_page")
        val lastParam    = Option(storage.getItem(Global.StoragePrefix + "last_param")).getOrElse("")
        val lastIsStatic = lastPage == pages.MainView.name.value && pages.MainView.isStaticPage(lastParam)
        val canRestore = startPage.isEmpty && Global.isHomePage && !lastIsStatic &&
                         lastWpPageId == Global.pageId.toString &&
                         lastPage != null && pages.pagesMap.contains(PageNameTyp(lastPage))

        if canRestore then pages.loadPage(PageNameTyp(lastPage), lastParam) else runDefaultRouting(startPage)

      case Left(err) =>
        debug(s"initApp -> Error loading user info: ${err}")
        Navbar.render()
        Footer.render()
        runDefaultRouting(startPage)
    }

  /**
   * Wählt die Startseite: Shortcode-Attribut page bzw. statische Seite, sonst MainView.
   *
   * @param startPage Optionale Startseite (login, register, verify, static:<datei>)
   */
  private def runDefaultRouting(startPage: String): Unit =
    startPage match {
      case "login"    => pages.loadPage(pages.UserLogin.name, "")
      case "register" => pages.loadPage(pages.UserRegistration.name, "")
      case "verify"   => pages.loadPage(pages.VerifyAccount.name, "")
      case s if s.startsWith("static:") => pages.loadPage(pages.MainView.name, s.stripPrefix("static:"))
      case _          => pages.loadPage(pages.MainView.name, "")
    }

  def handleGoogleCredential(credentials: String): Unit = pages.Auth.googleLogin(credentials)

  def appLoadPage(pageName: String, param: String): Unit =
    pages.loadPage(PageNameTyp(pageName), param)


  def render(param: String = ""): Boolean = true

  def appEvent(elem: HTMLElement, event: dom.Event): Unit =
    try
      val (name, key) = elem.id.toTuple("_")
      val pgDlgName = PageNameTyp(name)

      debug(s"event -> pgDlgName:${name} key:${key} elem:${elem.id}")

      if pages.pagesMap.contains(pgDlgName) then
        pages.pagesMap(pgDlgName).handleEvent(elem, event)
      else if comps.compsMap.contains(pgDlgName) then
        comps.compsMap(pgDlgName).handleEvent(elem, event)
      else
        dialogs.dlgMap(pgDlgName).handleEvent(elem, event)
    catch
      case e: Exception => error(s"event -> elem:${elem.id} failed")



  @JSExportTopLevel("getLogLevel")
  def getLogLevel():Option[String] =  Logging.getLogLevel()

  @JSExportTopLevel("setLogLevel")
  def setLogLevel(value: String="") = Logging.setLogLevel(value)
