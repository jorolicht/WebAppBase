package comps

import org.scalajs.dom
import base.Global
import base.Logging.*
import shared.model.{Text, MenuItem, Card}
import shared.PageNameTyp

/**
 * Führt die Aktionen der App-Konfiguration aus (Menüeinträge der Navbar, Karten der Startseite).
 *
 * Format der Aktion (siehe shared.model.MenuItem):
 * "page:<Seite>[:<Parameter>]", "static:<Datei>", "url:<Adresse>", "dialog:<Text>", leer = Mockup-Seite.
 */
object Actions:

  /**
   * Alle Menüeinträge der Konfiguration, inklusive der Untereinträge.
   *
   * @return Menüeinträge in Reihenfolge der Konfiguration
   */
  def allMenuItems: List[MenuItem] =
    def flatten(items: List[MenuItem]): List[MenuItem] = items.flatMap(i => i :: flatten(i.children))
    flatten(Global.appConfig.menu)

  /**
   * Sucht einen Menüeintrag über seine ID.
   *
   * @param id ID aus der Konfiguration
   * @return Menüeintrag oder None
   */
  def menuItem(id: String): Option[MenuItem] = allMenuItems.find(_.id == id)

  /**
   * Linkziel einer Aktion für das href-Attribut: eigene URL bei statischen Seiten und externen Links,
   * sonst "#" (die Aktion läuft über onclick).
   *
   * @param action Aktion aus der Konfiguration
   * @return URL
   */
  def href(action: String): String = action.split(":", 2).toList match
    case "static" :: file :: Nil => pages.MainView.staticUrl(file)
    case "url" :: url :: Nil     => url
    case _                       => "#"

  /**
   * True, wenn die Aktion einen externen Link in neuem Tab öffnet.
   *
   * @param action Aktion aus der Konfiguration
   */
  def isExternal(action: String): Boolean = action.startsWith("url:")

  /**
   * Führt eine Aktion aus.
   *
   * @param action Aktion aus der Konfiguration
   * @param id     ID des Menüeintrags bzw. der Karte (Parameter der Mockup-Seite)
   * @param title  Titel für Dummy-Dialog
   */
  def run(action: String, id: String, title: Text): Unit =
    debug(s"Actions.run -> action:${action} id:${id}")
    action.split(":", 2).toList match
      case "page" :: rest :: Nil =>
        val (page, param) = rest.split(":", 2).toList match
          case p :: prm :: Nil => (p, prm)
          case p :: _          => (p, "")
          case Nil             => ("MainView", "")
        pages.loadPage(PageNameTyp(page), param)
      case "static" :: file :: Nil => pages.loadPage(pages.MainView.name, file)
      case "url" :: url :: Nil     => dom.window.open(url, "_blank", "noopener")
      case "dialog" :: text :: Nil => dialogs.DlgMsgbox.show(text, title.get(Global.lang), List(shared.BoxButton.Close))
      case _                       => pages.loadPage(pages.Mockup.name, id)

  /**
   * Führt die Aktion eines Menüeintrags aus.
   *
   * @param item Menüeintrag
   */
  def run(item: MenuItem): Unit = run(item.action, item.id, item.label)

  /**
   * Führt die Aktion einer Karte aus.
   *
   * @param card Karte der Startseite
   */
  def run(card: Card): Unit = run(card.action, card.id, card.title)

  /**
   * Textfarbe mit ausreichendem Kontrast zu einer Hintergrundfarbe (relative Helligkeit nach WCAG).
   *
   * @param hex Hintergrundfarbe, z. B. "#4A607A"
   * @return "#212529" für helle, "#fff" für dunkle Hintergründe
   */
  def textColor(hex: String): String =
    try
      val h = hex.stripPrefix("#")
      def lin(i: Int): Double =
        val c = Integer.parseInt(h.substring(i, i + 2), 16) / 255.0
        if c <= 0.03928 then c / 12.92 else math.pow((c + 0.055) / 1.055, 2.4)
      val l = 0.2126 * lin(0) + 0.7152 * lin(2) + 0.0722 * lin(4)
      if l > 0.179 then "#212529" else "#fff"
    catch
      case _: Exception => "#fff"
