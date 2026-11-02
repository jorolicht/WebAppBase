# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projekt

- Vorlage für Web-Anwendungen auf Basis von WordPress: Scala 3 (3.3.7) Playframework Cross Project mit Scala.js Client (Twirl-Templates auch clientseitig) und WordPress-Plugin `webapp`, in dem der Scala.js Client verwendet wird. Abgeleitet aus Playdemo (Turnier-Service), ohne die turnierspezifischen Teile.
- Kommentiere alle Methoden und Objekte entsprechend den Dokumentierungsrichtlinien für Scala (Scaladoc) und WordPress (PHPDoc).
- `server/` – Playframework Server (JVM)
- `client/` – Scala.js Client mit `addon` (Debug-Console, `startConsole()` in der JavaScript-Console des Browsers)
- `shared/` – Code für Server und Client (crossProject, `CrossType.Pure`)
- `wp-plugin/` – WordPress-Plugin `webapp`; Inhalt wird beim Übersetzen nach `server/docker/<env>/wp_data/wp-content/plugins/webapp` kopiert
- `wp-plugin/config/app.json` – App-Konfiguration (Menü, Karten, Hero-Texte, statische Seiten), gelesen von Client und Plugin
- `server/docker/dev` – Docker-Setup der Development-Umgebung, `server/docker/prod` – Produktiv-Umgebung
- `site/content` – statische Seiten, werden nach `wp-plugin/pages` kopiert
- `test/e2e` – Playwright E2E-Tests, Katalog `test/TestCaseCatalog.md`

## Build & Befehle

sbt startet automatisch im Projekt `server` (`Global / onLoad`).

- `sbt compile` – kompiliert Server **und** löst `scalaJSPipeline` + `syncClientWpFiles` aus (fastLinkJS + fullLinkJS, Kopie von JS/CSS/PHP/config/pages nach `wp-plugin/` und ins Docker-Plugin-Verzeichnis)
- `sbt buildMsg` – Nachrichtendateien neu erzeugen (`genMsgFiles` → `convertMessagesToJson`)
- `sbt packageWpPlugin` (Alias `packageWp`) – installierbares ZIP `target/wordpress/webapp-<version>.zip`
- `sbt stage` / `sbt dist` – Play-Distribution, wird nach `server/docker/<env>/playsrv/` kopiert
- `sbt dockerBuildImages` / `dockerPushImages` – Images `jorolich/webapp-{playsrv,wp-cli,wp}-image` (linux/amd64)

**Umgebung:** Ziel-Docker-Verzeichnis über `APP_ENV` bzw. `-Dapp.env`; **Default ist `prod`**. Für Entwicklung: `APP_ENV=dev sbt compile`. `APP_VERSION`, `APP_DATE` etc. kommen aus `server/docker/<env>/.env`.

**Voraussetzung:** externes Tool `msgConverter` im `PATH` (für `convertMessagesToJson`); PHP-Abhängigkeiten über `composer install` in `wp-plugin/`.

**Dev-Umgebung:** `server/docker/dev/setup.sh` (erstmalig), danach `start.sh`/`stop.sh`. Website https://localhost:8443, MailCatcher http://localhost:1081. Container `webapp-*`, Netzwerke `webapp-proxy`/`webapp-backend`; Traefik routet über `dynamic_config.toml` (kein Docker-Provider), damit die Umgebung parallel zu Playdemo laufen kann.

### Tests

```
cd test/e2e
npx playwright test                                  # alle
npx playwright test tests/app_config.spec.ts         # eine Datei
npx playwright test -g "<Testname>" --project=chromium
```

## Architektur

- **Einstieg Client:** Shortcode `[webapp]` (`wp-plugin/webapp.php`) lädt `js/main.js` als ES-Modul und ruft `startApp(version, logLevel)` (`client/src/main/scala/Main.scala`). Konfiguration (dataurl, configurl, playurl, homeurl, nonce, pageid …) kommt aus data-Attributen des Param-Elements und liegt in `base.Global`; `app.json` wird beim Start nach `Global.appConfig` geladen.
- **Menüs/Karten:** `comps.Navbar` und `pages.MainView` rendern aus `Global.appConfig`; `comps.Actions` führt Aktionen aus (`page:`, `static:`, `url:`, `dialog:`, leer = `pages.Mockup`). IDs: `Navbar_Menu-<id>`, `MainView_Card-<id>`.
- **UI-Struktur Client:** `pages/`, `comps/`, `dialogs/` mit Twirl-Templates unter `client/src/main/twirl/cviews/`, registriert in `pages.pagesMap`, `comps.compsMap`, `dialogs.dlgMap`. HTML-Events laufen über `appEvent`; die Element-ID hat das Format `<Name>_<key>`. Seitenwechsel über `appLoadPage(pageName, param)`.
- **WordPress:** REST-Namespace `webapp/v1` (`wp-plugin/includes/api/*.php`), Auth per `X-WP-Nonce`/JWT. PHP-Präfixe `webapp_` bzw. `WEBAPP_`. Statische Seiten und Startseite stehen schon im Server-HTML (`includes/static-pages.php`, `includes/home-static.php`).
- **Play-Server** (`server/conf/routes`): Messages, WebSockets/SSE (Pekko-Actors), WordPress-Proxy (`/wp/post`).
- **i18n:** Nur die Teildateien `server/conf/messages/{de,en}/*.{de,en}` bearbeiten; `00_info.*`, `messages.{de,en}` und `msgs_{de,en}.json` sind generiert.
- **Generierte/kopierte Dateien nicht direkt bearbeiten:** `wp-plugin/js`, `wp-plugin/pages`, `wp-plugin/data`, alles unter `server/docker/*/wp_data`.

## Arbeitsweise

- Think before coding. State your assumptions out loud. If the request is ambiguous, ask. If a simpler approach exists, push back.
- Simplicity first. Write the minimum code that solves the problem. No speculative abstractions.
- Surgical changes. Touch only what the task requires.
- Goal-driven execution. Turn vague instructions into verifiable targets before writing a line.
- Keine `.env`-Dateien oder echten Passwörter einchecken; `.env.example` nur mit Platzhaltern.
