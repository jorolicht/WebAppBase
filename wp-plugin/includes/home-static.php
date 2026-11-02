<?php

/**
 * Serverseitige Fassung der Startseite (Progressive Enhancement, SEO).
 *
 * Statt "Wird geladen..." liefert das Plugin auf der Startseite schon im HTML die Inhalte von MainView
 * (Überschrift, Beschreibung, Karten). Der Scala.js-Client ersetzt den Inhalt von #Main_ContentId beim Start
 * durch die vollständige Startseite (client/src/main/twirl/cviews/pages/MainView/Base.scala.html).
 *
 * Die Texte stammen aus derselben Quelle wie im Client (config/app.json), damit Server- und Client-Inhalt
 * übereinstimmen (kein Cloaking).
 */

/**
 * Textfarbe mit ausreichendem Kontrast zu einer Hintergrundfarbe (wie comps.Actions.textColor im Client).
 *
 * @param string $hex Hintergrundfarbe, z. B. "#4A607A".
 * @return string "#212529" für helle, "#fff" für dunkle Hintergründe.
 */
function webapp_text_color($hex) {
    $h = ltrim($hex, '#');
    if (strlen($h) !== 6) {
        return '#fff';
    }
    $lin = function ($i) use ($h) {
        $c = hexdec(substr($h, $i, 2)) / 255;
        return $c <= 0.03928 ? $c / 12.92 : pow(($c + 0.055) / 1.055, 2.4);
    };
    $l = 0.2126 * $lin(0) + 0.7152 * $lin(2) + 0.0722 * $lin(4);
    return $l > 0.179 ? '#212529' : '#fff';
}

/**
 * Erzeugt das HTML der Startseite für #Main_ContentId.
 *
 * @return string HTML der Startseite oder leerer String, wenn die Konfiguration fehlt.
 */
function webapp_home_static_html() {
    $config = webapp_config();
    $hero   = $config['hero'] ?? [];
    if (empty($hero)) {
        return '';
    }
    $t = fn($text) => esc_html(webapp_config_text($text ?? []));

    $html  = '<div class="main-multi-landing">';
    $html .= '<div class="hero-section text-white d-flex align-items-center justify-content-center text-center shadow-sm" style="min-height:18vh;background-color:#336699;">';
    $html .= '<div class="hero-content px-3 py-3">';
    $html .= '<h1 class="h2 fw-bold mb-1">' . $t($hero[0] ?? null) . '</h1>';
    $html .= '<p class="lead mb-1 fs-6">' . $t($hero[1] ?? null) . '</p>';
    $html .= '<p class="small opacity-75 mx-auto mb-0" style="max-width:600px;">' . $t($hero[2] ?? null) . '</p>';
    $html .= '</div></div>';
    $html .= '<div class="container mt-4 pb-4"><div class="row g-4 justify-content-center">';
    foreach ($config['cards'] ?? [] as $card) {
        $color = $card['color'] ?? '#336699';
        $style = 'background-color:' . esc_attr($color) . ';color:' . webapp_text_color($color) . ';';
        $html .= '<div class="col-lg-3 col-md-6"><div class="card h-100 border-0 shadow"><div class="card-body text-center p-4 d-flex flex-column">';
        $html .= '<div class="bg-gradient mb-3 mx-auto rounded-circle d-flex align-items-center justify-content-center" style="width:60px;height:60px;' . $style . '"><i class="bi ' . esc_attr($card['icon'] ?? 'bi-app') . ' fs-3"></i></div>';
        $html .= '<h3 class="h4 mb-3">' . $t($card['title'] ?? null) . '</h3>';
        $html .= '<p class="text-muted small">' . $t($card['desc'] ?? null) . '</p>';
        // Ohne JavaScript ohne Funktion: nur als Platzhalter, damit sich das Layout beim Start nicht verschiebt
        $html .= '<span class="btn mt-auto fw-bold disabled" style="' . $style . '" aria-hidden="true">' . $t($card['btn'] ?? null) . '</span>';
        $html .= '</div></div></div>';
    }
    $html .= '</div></div></div>';
    return $html;
}
