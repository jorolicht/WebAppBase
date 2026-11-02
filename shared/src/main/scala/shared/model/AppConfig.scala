package shared.model

import shared.basic.Pickle.{ReadWriter => RW, macroRW, *}

/**
 * Zweisprachiger Text der App-Konfiguration.
 *
 * @param de Deutscher Text
 * @param en Englischer Text (leer = deutscher Text)
 */
case class Text(de: String = "", en: String = ""):
  /** Text in der Sprache lang ("en" englisch, sonst deutsch). */
  def get(lang: String): String = if (lang == "en" && en.nonEmpty) en else de

object Text:
  implicit val rw: RW[Text] = macroRW

/**
 * Menüeintrag der Navbar (wp-plugin/config/app.json, Abschnitt "menu").
 *
 * Die Aktion wird als Präfix-String angegeben:
 *  - "page:<Seite>[:<Parameter>]" lädt eine registrierte Seite (pages.pagesMap)
 *  - "static:<Datei>"             öffnet eine statische Seite aus site/content
 *  - "url:<Adresse>"              öffnet einen externen Link in neuem Tab
 *  - "dialog:<Text>"              zeigt einen Dummy-Dialog mit dem Text
 *  - leer                         öffnet die Mockup-Seite des Eintrags
 *
 * @param id       Eindeutige ID (Teil der HTML-ID Navbar_<id>)
 * @param label    Beschriftung
 * @param icon     Bootstrap-Icon (z. B. "bi-gear"), optional
 * @param action   Aktion, siehe oben
 * @param desc     Beschreibung für die Mockup-Seite
 * @param visible  Sichtbarkeit: all, guest, user oder admin
 * @param children Untereinträge (Dropdown); dann wird action ignoriert
 */
case class MenuItem(
  id:       String,
  label:    Text,
  icon:     String = "",
  action:   String = "",
  desc:     Text = Text(),
  visible:  String = "all",
  children: List[MenuItem] = Nil
)

object MenuItem:
  implicit val rw: RW[MenuItem] = macroRW

/**
 * Karte der Startseite (MainView), Aktion wie bei MenuItem.
 *
 * @param id     Eindeutige ID
 * @param title  Überschrift
 * @param desc   Beschreibung
 * @param btn    Beschriftung des Buttons
 * @param icon   Bootstrap-Icon
 * @param color  Farbe von Icon und Button (Hex, z. B. "#E6CA83")
 * @param action Aktion, siehe MenuItem
 */
case class Card(
  id:     String,
  title:  Text,
  desc:   Text = Text(),
  btn:    Text = Text(),
  icon:   String = "bi-app",
  color:  String = "#336699",
  action: String = ""
)

object Card:
  implicit val rw: RW[Card] = macroRW

/**
 * Statische Seite mit eigener URL (Inhalt aus site/content/<file>.html bzw. <file>_en.html).
 *
 * @param file    Dateiname ohne Endung und Sprachsuffix, zugleich Seitenname im Client
 * @param slug    URL-Slug deutsch/englisch (englisch unter /en/<slug>/)
 * @param title   Seitentitel
 * @param desc    Meta-Beschreibung (SEO)
 * @param footer  True, wenn die Seite im Footer verlinkt wird
 */
case class StaticPage(
  file:   String,
  slug:   Text,
  title:  Text,
  desc:   Text = Text(),
  footer: Boolean = false
)

object StaticPage:
  implicit val rw: RW[StaticPage] = macroRW

/**
 * Gesamte App-Konfiguration aus wp-plugin/config/app.json; wird von Client und Plugin (PHP) gelesen.
 *
 * @param brand       Name der Anwendung (Navbar, Footer)
 * @param hero        Überschrift, Untertitel und Beschreibung der Startseite
 * @param menu        Menüeinträge der Navbar (links)
 * @param cards       Karten der Startseite
 * @param staticPages Statische Seiten mit eigener URL
 */
case class AppConfig(
  brand:       Text = Text("WebAppBase"),
  hero:        List[Text] = Nil,
  menu:        List[MenuItem] = Nil,
  cards:       List[Card] = Nil,
  staticPages: List[StaticPage] = Nil
)

object AppConfig:
  implicit val rw: RW[AppConfig] = macroRW
