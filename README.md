# WebAppBase

Vorlage für Web-Anwendungen auf Basis von WordPress: ein Scala.js-Client als Single-Page-Application, eingebettet in ein
WordPress-Plugin, dazu ein Play-Server für Hilfsdienste (WebSocket/SSE, WordPress-Proxy). Entstanden aus dem Projekt
Turnier-Service, ohne dessen fachliche Teile.

Enthalten sind die allgemeinen Funktionen:

- Navbar, Footer und Startseite, **konfigurierbar** über `wp-plugin/config/app.json`
- Generische **Mockup-Seite** für Menüpunkte ohne eigene Umsetzung
- Anmeldung mit E-Mail/Passwort oder Passkey (WebAuthn), Registrierung mit E-Mail-Bestätigung, Abmelden
- Verwaltungsseite für Administratoren (Systeminfo, Play-Server-Version, WebSocket-Test)
- Dialoge (Meldung mit Details, Debug-Konsole), Fehleranzeige, i18n (Deutsch/Englisch)
- Statische Seiten mit eigenen URLs (`/impressum/`, `/en/imprint/` …), SEO (Server-HTML, Meta-Daten, Sitemap)
- Docker-Umgebungen für Entwicklung und Produktion, Playwright-E2E-Tests

## Aufbau

| Verzeichnis | Inhalt |
|---|---|
| `client/` | Scala.js-Client: `pages/`, `comps/`, `dialogs/`, `services/`, Twirl-Templates unter `src/main/twirl/cviews/`; `addon/` enthält die Debug-Konsole (`startConsole()` in der Browser-Konsole) |
| `shared/` | Code für Client und Server (Modell, u. a. `shared.model.AppConfig`) |
| `server/` | Play-Server (`/srv`), Nachrichtendateien `conf/messages/{de,en}`, Docker-Umgebungen `docker/dev` und `docker/prod` |
| `wp-plugin/` | WordPress-Plugin `webapp` (Shortcode `[webapp]`, REST-Namespace `webapp/v1`) |
| `site/content/` | Statische Seiten (`<datei>.html`, englisch `<datei>_en.html`) |
| `test/e2e/` | Playwright-Tests, Katalog in `test/TestCaseCatalog.md` |

## Schnellstart (Entwicklung)

Voraussetzungen: JDK 17+, sbt, Docker, Node.js (für die Tests) und das Tool `msgConverter` im `PATH`.

```bash
# 1. PHP-Abhängigkeiten des Plugins (JWT, WebAuthn)
docker run --rm -v "$PWD/wp-plugin:/app" composer:2 install

# 2. Umgebung: Vorlage kopieren und alle CHANGE_ME ersetzen; lokale TLS-Zertifikate nach server/docker/dev/certs
#    (local-cert.pem, local-key.pem, z. B. mit mkcert localhost)
cp server/docker/dev/.env.example server/docker/dev/.env

# 3. Bauen: Client, Nachrichten, Plugin-Dateien ins Docker-Verzeichnis, Play-Server stagen
APP_ENV=dev sbt buildMsg compile stage

# 4. Container starten, WordPress installieren, Plugin aktivieren, Seiten anlegen
cd server/docker/dev && ./setup.sh
```

Danach: Website <https://localhost:8443>, Play-Server <https://localhost:8443/srv>, MailCatcher <http://localhost:1081>.
Die Ports stehen in der `.env` (`HTTP_PORT`, `HTTPS_PORT`, `MAILCATCHER_PORT`), Container und Netzwerke heißen `webapp-*`;
so läuft die Umgebung parallel zu anderen Projekten. Weitere Starts: `server/docker/dev/start.sh` bzw. `stop.sh`.

## Konfiguration: Menü, Karten, statische Seiten

`wp-plugin/config/app.json` wird vom Client (`shared.model.AppConfig`) und vom Plugin (`includes/app-config.php`) gelesen.
Texte sind zweisprachig: `{ "de": "…", "en": "…" }`.

| Abschnitt | Bedeutung |
|---|---|
| `brand` | Name in Navbar und Footer |
| `hero` | Überschrift, Untertitel und Beschreibung der Startseite (auch Titel/Description für SEO) |
| `menu` | Menüeinträge der Navbar; mit `children` als Dropdown |
| `cards` | Karten der Startseite mit Icon, Farbe (`color`, Hex) und Button |
| `staticPages` | Statische Seiten: Datei in `site/content`, Slug und Titel je Sprache, `footer: true` für den Footer-Link |

Menüeinträge und Karten haben eine `action`:

| Aktion | Wirkung |
|---|---|
| `page:<Seite>[:<Parameter>]` | lädt eine registrierte Seite (`pages.pagesMap`), z. B. `page:UserLogin` |
| `static:<datei>` | öffnet eine statische Seite mit eigener URL |
| `url:<adresse>` | externer Link in neuem Tab |
| `dialog:<text>` | Dummy-Dialog mit dem Text |
| *(leer)* | Mockup-Seite mit Titel, Icon und `desc` des Eintrags |

Mit `visible` (`all`, `guest`, `user`, `admin`) werden Menüeinträge je nach Anmeldung ein- oder ausgeblendet.
Nach Änderungen genügt `sbt compile` (kopiert die Datei ins Plugin); die Startseite im Server-HTML liest dieselbe Datei.

## Eine Mockup-Funktion umsetzen

1. Seite anlegen: `client/src/main/scala/pages/<Name>.scala` (`object <Name> extends BasePage`) und Template
   `client/src/main/twirl/cviews/pages/<Name>.scala.html`.
2. In `pagesMap` (`client/src/main/scala/pages/BasePage.scala`) registrieren.
3. In `app.json` die Aktion des Menüeintrags auf `page:<Name>` setzen.
4. Texte in `server/conf/messages/{de,en}/*.{de,en}` ergänzen (`sbt buildMsg`), Daten über eigene REST-Endpunkte
   unter `wp-plugin/includes/api/` (Namespace `webapp/v1`).

HTML-Events laufen über die globale Funktion `appEvent`; die Element-ID hat das Format `<Komponente>_<key>` und wird an
`handleEvent` der Komponente weitergeleitet.

## sbt-Befehle

| Befehl | Wirkung |
|---|---|
| `sbt compile` | Server und Client übersetzen, Plugin-Dateien nach `wp-plugin/` und ins Docker-Plugin-Verzeichnis kopieren |
| `sbt buildMsg` | Nachrichtendateien und `msgs_{de,en}.json` erzeugen |
| `sbt packageWp` | installierbares Plugin-ZIP `target/wordpress/webapp-<version>.zip` |
| `sbt stage` | Play-Distribution nach `server/docker/<env>/playsrv/stage` |
| `sbt dockerBuildImages` / `dockerPushImages` | Images `jorolich/webapp-{playsrv,wp-cli,wp}-image` (linux/amd64) |

Die Zielumgebung wählt `APP_ENV` (`dev` oder `prod`, Default `prod`); Version und Datum kommen aus `server/docker/<env>/.env`.

## Tests

```bash
cd test/e2e
cp .env.example .env   # WEBAPP_ADMIN_PASSWORD eintragen
npm ci
npx playwright test --project=chromium
```

## Neue Anwendung aus der Vorlage

1. Repository kopieren und umbenennen; Plugin-Slug in `build.sbt` (`pluginSlug`) und den Dateinamen `wp-plugin/webapp.php` anpassen.
2. Präfixe `webapp_`/`WEBAPP_` (PHP), REST-Namespace `webapp/v1`, Container-Namen `webapp-*` und Ports in `server/docker/*` ersetzen.
3. `app.json`, statische Seiten, Logo (`wp-plugin/img`) und Texte anpassen.

## Sicherheit

`.env`-Dateien mit Zugangsdaten werden nie eingecheckt; `.env.example` enthält nur Platzhalter.

## Lizenz

GPLv2 or later (wie das WordPress-Plugin, siehe `wp-plugin/readme.txt`).
