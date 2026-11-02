# Testkatalog WebAppBase

E2E-Tests (Playwright) in `test/e2e/tests`. Markierung: 🌐 ohne Anmeldung, 👤 mit Anmeldung.

Voraussetzung: laufende Dev-Umgebung (`server/docker/dev/setup.sh`), Konfiguration in `test/e2e/.env` (Vorlage `.env.example`).

```
cd test/e2e
npm ci
npx playwright test --project=chromium
```

| ID | | Beschreibung | Datei |
|---|---|---|---|
| TC-AU-01 | 🌐 | Regulärer Login mit E-Mail & Passwort | user_login_ratelimit.spec.ts |
| TC-AU-02 | 👤 | Abmelden (Logout) | user_login_ratelimit.spec.ts |
| TC-AU-03 | 🌐 | Registrierungsseite Aufruf (UserRegistration) | user_login_ratelimit.spec.ts |
| TC-AU-04 | 🌐 | Fehllogin-Handling & Dynamisches Captcha / Rate-Limiting | user_login_ratelimit.spec.ts |
| TC-CB-01 | 🌐 | Mobile Viewport (375x667) & Hamburger-Menü | cross_browser_responsive.spec.ts |
| TC-CB-02 | 🌐 | Tablet Viewport (768x1024) & Grid-Anpassung | cross_browser_responsive.spec.ts |
| TC-CB-03 | 🌐 | Desktop Viewport (1280x800) & Nebeneinander-Darstellung | cross_browser_responsive.spec.ts |
| TC-CB-04 | 🌐 | Hoch- / Querformat-Umschaltung (Portrait vs Landscape) | cross_browser_responsive.spec.ts |
| TC-CFG-01 | 🌐 | Navbar und Karten kommen aus config/app.json | app_config.spec.ts |
| TC-CFG-02 | 🌐 | Startseite steht schon im Server-HTML (SEO) | app_config.spec.ts |
| TC-CFG-03 | 🌐 | Menüeintrag ohne Aktion öffnet die Mockup-Seite | app_config.spec.ts |
| TC-CFG-04 | 🌐 | Sichtbarkeit "user" blendet Einträge für Gäste aus | app_config.spec.ts |
| TC-CFG-05 | 🌐 | Aktion static: öffnet die statische Seite mit eigener URL | app_config.spec.ts |
| TC-CFG-06 | 🌐 | Aktion url: verlinkt extern in neuem Tab | app_config.spec.ts |
| TC-CFG-07 | 🌐 | Aktion page: der Karte öffnet die Seite | app_config.spec.ts |
| TC-CFG-08 | 🌐 | Englische Texte unter /en/ | app_config.spec.ts |
| TC-CFG-09 | 👤 | Verwaltung zeigt Play-Server-Version und WebSocket-Antwort | app_config.spec.ts |
| TC-ERR-01 | 🌐 | WordPress-Fehler ohne Übersetzung zeigt den Server-Text, Details mit Code und CallStack | error_dialog.spec.ts |
| TC-ERR-02 | 🌐 | Übersetzter Fehler zeigt den Text mit eingesetzten Inserts | error_dialog.spec.ts |
| TC-FT-01 | 🌐 | Buttons erscheinen genau für die konfigurierten Netzwerke mit korrektem Link | footer_social.spec.ts |
| TC-LANG-01 | 🌐 | /en/ und /en/<seite>/ liefern Englisch mit lang, Canonical, hreflang und Sitemap | lang_urls.spec.ts |
| TC-LANG-02 | 🌐 | Besuch von /en/ merkt Englisch; deutsche URL zeigt Deutsch, Seiten ohne Sprach-URL die gemerkte Sprache | lang_urls.spec.ts |
| TC-LANG-03 | 🌐 | Umschalter führt zur Gegenstück-URL und merkt die Wahl | lang_urls.spec.ts |
| TC-LANG-04 | 🌐 | Hinweis auf die englische Fassung nur für englischsprachige Besucher ohne gemerkte Wahl | lang_urls.spec.ts |
| TC-NR-01 | 🌐 | Injektion von Server-Fehlern (HTTP 500 / 503) | network_resilience.spec.ts |
| TC-NR-02 | 🌐 | Injektion von Rate-Limiting (HTTP 429 Too Many Requests) | network_resilience.spec.ts |
| TC-NR-03 | 🌐 | Simulation von hoher Netzwerk-Latenz (Throttling / Delay) | network_resilience.spec.ts |
| TC-REG-01 | 🌐 | Mailcatcher REST-API ist erreichbar und einsatzbereit | user_registration_mailcatcher.spec.ts |
| TC-REG-02 | 🌐 | Passwortbasierte Registrierung, Empfang der Aktivierungsmail in Mailcatcher, Verifizierung & Login | user_registration_mailcatcher.spec.ts |
| TC-REG-03 | 🌐 | Passwortlose Registrierung, E-Mail-Empfang und Aktivierung via Verifizierungslink | user_registration_mailcatcher.spec.ts |
| TC-REG-05 | 🌐 | Passwortlose Registrierung mit Passkey nach An- und Abmelden im selben Tab (gültiger Nonce) | user_registration_mailcatcher.spec.ts |
| TC-SEO-01 | 🌐 | Startseite hat genau einen Titel, Description, Open Graph, Twitter Card und JSON-LD | seo.spec.ts |
| TC-SEO-02 | 🌐 | Login, Registrierung, Verifizierung und Vorlagen sind noindex und nicht in der Sitemap | seo.spec.ts |
| TC-SEO-03 | 🌐 | Keine Benutzer-Sitemap, Autorenarchive leiten auf die Startseite, keine öffentliche Benutzerliste | seo.spec.ts |
| TC-SEO-05 | 🌐 | Startseite liefert ihren Inhalt im Server-HTML, der Client ersetzt ihn beim Start | seo.spec.ts |
| TC-SM-01 | 🌐 | Session-Management & Storage-Clearance bei Logout | session_security_guards.spec.ts |
| TC-SM-02 | 🌐 | Route Guard #Management (Zugriffsschutz für geschützte Admin-URL) | session_security_guards.spec.ts |
| TC-STATIC-01 | 🌐 | Jede URL liefert 200, Titel, Canonical und den Inhalt im Server-HTML und steht in der Sitemap | static_pages.spec.ts |
| TC-STATIC-02 | 🌐 | Direkter Aufruf öffnet die Seite in der App | static_pages.spec.ts |
| TC-STATIC-03 | 🌐 | Footer- und Navbar-Links haben echte URLs; Klick ändert die Adresszeile, Zurück führt zur Startseite | static_pages.spec.ts |
