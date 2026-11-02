package pages

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.Event
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import shared.basic.Pickle.*
import shared.model.StaticPage
import services.*
import base.*

/**
 * Startseite (Hero, News-Zeile, Karten aus der App-Konfiguration) sowie Anzeige statischer Seiten
 * (site/content) und WordPress-Seiten.
 */
object MainView extends BasePage with JsWrapper with ComWrapper:
  /** Name identifier for the page. */
  def name = PageNameTyp("MainView")

  /** HTML-ID for the news bar container below the hero section. */
  val NewsBarId:     HtmlId = genId(name)
  /** HTML-ID for the close button ('x') in the news bar. */
  val BtnCloseNews:  HtmlId = genId(name)
  /** Präfix der HTML-IDs der Karten-Buttons: MainView_Card-<id>. */
  val CardPrefix = "Card-"

  /** Statische Seiten mit eigener URL aus der App-Konfiguration. */
  def staticPages: List[StaticPage] = Global.appConfig.staticPages

  /**
   * Normalisiert einen Seitenparameter (Dateiname oder deutscher/englischer Slug, z. B. "preise") auf den Dateinamen.
   *
   * @param param Seitenparameter von appLoadPage('MainView', param).
   * @return Dateiname ohne Endung und Sprachsuffix.
   */
  def staticBaseName(param: String): String =
    val p = param.toLowerCase
    staticPages.find(sp => sp.file == p || sp.slug.de == p || sp.slug.en == p).map(_.file).getOrElse(p)

  /**
   * Prüft, ob der Parameter eine statische Seite mit eigener URL bezeichnet.
   *
   * @param param Seitenparameter.
   * @return True für statische Seiten.
   */
  def isStaticPage(param: String): Boolean = staticPages.exists(_.file == staticBaseName(param))

  /**
   * URL einer statischen Seite in einer Sprache, z. B. https://example.com/preise/ bzw. …/en/pricing/.
   *
   * @param param Seitenparameter; leer bzw. unbekannt für die Startseite (/ bzw. /en/).
   * @param lang  Sprache ("en" für die englische Fassung, sonst deutsch).
   * @return Absolute URL.
   */
  def staticUrlFor(param: String, lang: String): String =
    val prefix = if (lang == "en") "en/" else ""
    val base = staticBaseName(param)
    val slug = staticPages.find(_.file == base).map(sp => (if (lang == "en") sp.slug.en else sp.slug.de) + "/").getOrElse("")
    s"${Global.homeUrl.stripSuffix("/")}/$prefix$slug"

  /**
   * URL einer statischen Seite in der aktuellen Sprache (für href und history.pushState).
   *
   * @param param Seitenparameter; leer für die Startseite.
   * @return Absolute URL.
   */
  def staticUrl(param: String): String = staticUrlFor(param, Global.lang)

  /**
   * Ermittelt die statische Seite zu einem URL-Pfad (z. B. "/preise/" oder "/en/pricing/" -> "pricing").
   *
   * @param path URL-Pfad (location.pathname).
   * @return Dateiname der Seite oder None.
   */
  def staticPageOfPath(path: String): Option[String] =
    val rel = path.stripPrefix(new dom.URL(staticUrlFor("", "de")).pathname).stripSuffix("/")
    if (rel.startsWith("en/")) staticPages.find(_.slug.en == rel.stripPrefix("en/")).map(_.file)
    else staticPages.find(_.slug.de == rel).map(_.file)

  /**
   * Prüft, ob ein URL-Pfad eine Startseite ist (/ oder /en/).
   *
   * @param path URL-Pfad.
   * @return True für die deutsche oder englische Startseite.
   */
  def isHomePath(path: String): Boolean =
    Seq("de", "en").exists(lang => new dom.URL(staticUrlFor("", lang)).pathname == path)

  /**
   * URL der aktuellen Seite in einer anderen Sprache: Gegenstück der statischen Seite bzw. Startseite,
   * sonst die aktuelle URL (App-Seiten haben keine Sprach-URL).
   *
   * @param lang Zielsprache.
   * @return Absolute URL.
   */
  def langSwitchUrl(lang: String): String =
    val path = dom.window.location.pathname
    staticPageOfPath(path) match
      case Some(page)                => staticUrlFor(page, lang)
      case None if isHomePath(path)  => staticUrlFor("", lang)
      case None                      => dom.window.location.href

  /**
   * Wechselt die Sprache: merkt sie im localStorage und lädt die Seite in der Zielsprache neu
   * (die Texte werden beim Start geladen). Ist die Seite schon in der Zielsprache, wird nur der Hinweis ausgeblendet.
   *
   * @param lang Zielsprache ("de" oder "en").
   */
  def switchLang(lang: String): Unit =
    try dom.window.localStorage.setItem(Global.LangStorageKey, lang) catch { case _: Throwable => () }
    Global.showLangHint = false
    if (lang == Global.lang) comps.Navbar.render()
    else
      val target = langSwitchUrl(lang)
      if (target == dom.window.location.href) dom.window.location.reload() else dom.window.location.assign(target)

  /** Models for standard WordPress REST API response parsing. */
  case class WpContent(rendered: String) derives ReadWriter
  case class WpTitle(rendered: String) derives ReadWriter
  case class WpPage(id: Int, title: WpTitle, content: WpContent, translations: Option[Map[String, Int]] = None) derives ReadWriter

  /**
   * Basis-URL des Plugin-Verzeichnisses (aus dataUrl …/plugins/webapp/data/ abgeleitet).
   *
   * @return URL mit abschließendem "/"
   */
  def pluginUrl: String = Global.dataUrl.replaceAll("data/?$", "")

  /**
   * Renders the home view (empty param) or the static content / WordPress page specified by param.
   *
   * @param param Page slug, ID, or page identifier.
   * @return Boolean true on render.
   */
  def render(param: String = ""): Boolean =
    if (param.isEmpty) {
      setMain(cviews.pages.MainView.html.Base(Global.appConfig))
      loadLatestHistoryEntry()
    } else {
      setMain(cviews.pages.MainView.html.Loading(param))

      if (isStaticPage(param)) {
        loadStaticPage(param)
      } else {
        // WordPress-Seite über ID oder Slug laden
        param.toIntOption match {
          case Some(pageId) =>
            ajaxGet[WpPage](s"/wp-json/wp/v2/pages/$pageId", List(), host = Global.homeUrl).map {
              case Right(page) =>
                setMain(cviews.pages.MainView.html.PageContent(page.title.rendered, page.content.rendered))
              case Left(err) =>
                setMain(cviews.pages.MainView.html.PageNotFound(pageId.toString, err.msgCode))
            }
          case None =>
            ajaxGet[Seq[WpPage]](s"/wp-json/wp/v2/pages?slug=$param", List(), host = Global.homeUrl).map {
              case Right(pages) if pages.nonEmpty =>
                val page = pages.head
                page.translations.flatMap(_.get(Global.lang)) match {
                  case Some(transId) if transId != page.id =>
                    ajaxGet[WpPage](s"/wp-json/wp/v2/pages/$transId", List(), host = Global.homeUrl).map {
                      case Right(transPage) =>
                        setMain(cviews.pages.MainView.html.PageContent(transPage.title.rendered, transPage.content.rendered))
                      case Left(_) =>
                        setMain(cviews.pages.MainView.html.PageContent(page.title.rendered, page.content.rendered))
                    }
                  case _ =>
                    setMain(cviews.pages.MainView.html.PageContent(page.title.rendered, page.content.rendered))
                }
              case _ =>
                loadStaticPage(param)
            }
        }
      }
    }
    true

  /**
   * Lädt pages/<datei>[_en].html aus dem Plugin-Verzeichnis (mit Cache-Busting).
   *
   * @param file Dateiname ohne Endung und Sprachsuffix
   * @return Seiteninhalt oder None
   */
  private def fetchPage(file: String): scala.concurrent.Future[Option[String]] =
    val langSuffix = if (Global.lang == "en") "_en" else ""
    val urls = List(s"${pluginUrl}pages/$file$langSuffix.html", s"${pluginUrl}pages/$file.html").distinct
    def tryFetch(rest: List[String]): scala.concurrent.Future[Option[String]] = rest match
      case Nil => scala.concurrent.Future.successful(None)
      case url :: tail =>
        dom.fetch(s"$url?_t=${dom.window.performance.now().toLong}").toFuture
          .flatMap(resp => if resp.ok then resp.text().toFuture.map(Some(_)) else tryFetch(tail))
          .recoverWith { case _ => tryFetch(tail) }
    tryFetch(urls)

  /**
   * Liest den neuesten Eintrag (.history-entry) der Versionshistorie und zeigt ihn in der News-Zeile.
   */
  def loadLatestHistoryEntry(): Unit =
    fetchPage("history").foreach {
      case Some(htmlText) =>
        val tempDiv = dom.document.createElement("div")
        tempDiv.innerHTML = htmlText
        Option(tempDiv.querySelector(".history-entry")).foreach { entry =>
          def attr(key: String) = Option(entry.getAttribute(s"data-$key")).getOrElse("")
          Option(dom.document.getElementById("MainView_NewsVersion")).foreach(_.textContent = s"v${attr("version")}")
          Option(dom.document.getElementById("MainView_NewsDate")).foreach(_.textContent = s"(${attr("date")})")
          Option(dom.document.getElementById("MainView_NewsSummary")).foreach(_.textContent = attr("summary"))
          removeClass(gE(NewsBarId), "d-none")
        }
      case None => ()
    }

  /**
   * Zeigt eine statische Seite mit dem Titel aus der App-Konfiguration an.
   *
   * @param param Dateiname oder Slug der Seite.
   */
  private def loadStaticPage(param: String): Unit =
    val file = staticBaseName(param)
    fetchPage(file).foreach {
      case Some(text) =>
        val title = staticPages.find(_.file == file).map(_.title.get(Global.lang)).getOrElse(file.capitalize)
        setMain(cviews.pages.MainView.html.PageContent(title, text))
      case None =>
        setMain(cviews.pages.MainView.html.PageNotFound(param, "Keine Seite mit diesem Slug gefunden (404)."))
    }

  /**
   * Event handler for button clicks on MainView (News-Zeile schließen, Karten-Buttons).
   *
   * @param elem Target HTML element.
   * @param event DOM event trigger.
   */
  override def handleEvent(elem: HTMLElement, event: Event): Unit =
    val key = elem.id.stripPrefix(s"${name.value}_")
    if HtmlId(elem.id) == BtnCloseNews then addClass(gE(NewsBarId), "d-none")
    else if key.startsWith(CardPrefix) then
      Global.appConfig.cards.find(_.id == key.stripPrefix(CardPrefix)).foreach(comps.Actions.run)
    else debug(s"MainView handleEvent: ${elem.id}")
