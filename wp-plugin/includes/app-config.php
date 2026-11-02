<?php

/**
 * App-Konfiguration aus config/app.json (Menü, Karten, Hero-Texte, statische Seiten).
 *
 * Dieselbe Datei liest auch der Scala.js-Client (shared.model.AppConfig); das Plugin nutzt daraus
 * Hero-Texte und Karten (Startseite im Server-HTML), statische Seiten (eigene URLs) und den Namen (SEO).
 */

/**
 * Liest config/app.json (einmal pro Anfrage).
 *
 * @return array Konfiguration; leeres Array, wenn die Datei fehlt oder ungültig ist.
 */
function webapp_config() {
    static $config = null;
    if ($config === null) {
        $file   = WEBAPP_PLUGIN_PATH . 'config/app.json';
        $config = file_exists($file) ? (json_decode(file_get_contents($file), true) ?: []) : [];
    }
    return $config;
}

/**
 * Liefert einen zweisprachigen Text der Konfiguration ({"de": …, "en": …}) in der Sprache der URL.
 *
 * @param array       $text Text-Objekt aus der Konfiguration.
 * @param string|null $lang Sprache ('de' oder 'en'); null = Sprache der aktuellen URL.
 * @return string Text; der deutsche Text, wenn der englische fehlt.
 */
function webapp_config_text($text, $lang = null) {
    $lang = $lang ?? webapp_lang();
    return ($lang === 'en' && !empty($text['en'])) ? $text['en'] : ($text['de'] ?? '');
}
