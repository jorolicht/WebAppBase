=== WebAppBase ===
Contributors: rolicht
Tags: scala.js, single page application, template
Requires at least: 6.0
Tested up to: 6.7
Requires PHP: 8.0
Stable tag: 0.1.0
License: GPLv2 or later
License URI: https://www.gnu.org/licenses/gpl-2.0.html

Vorlage für Web-Anwendungen auf Basis von WordPress mit Scala.js-Client und Play-Server.

== Description ==

WebAppBase verbindet eine Single-Page-Application (Scala.js) mit WordPress und einem Play-Server.
Menüs, Startseiten-Karten und statische Seiten werden über `config/app.json` konfiguriert;
Menüeinträge ohne eigene Umsetzung öffnen eine generische Mockup-Seite.

* Shortcode `[webapp]`, optional `[webapp page="login|register|verify"]`
* Anmeldung mit E-Mail/Passwort oder Passkey, Registrierung mit E-Mail-Bestätigung
* Zweisprachig (Deutsch/Englisch) mit eigenen URLs für statische Seiten (`/en/...`)
* SEO: Startseite und statische Seiten bereits im Server-HTML, Meta-Daten, Sitemap

== Installation ==

1. Plugin-ZIP (`sbt packageWp`) im WordPress-Admin hochladen und aktivieren.
2. Unter Einstellungen → WebApp-Konfigurator die URL des Play-Servers eintragen.
3. Den Shortcode `[webapp]` in die Startseite einfügen.

== Changelog ==

= 0.1.0 =
* Erste Version der Vorlage.
