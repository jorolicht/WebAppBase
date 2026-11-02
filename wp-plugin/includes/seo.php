<?php

/**
 * Suchmaschinen-Optimierung.
 *
 * - Seitentitel, Meta-Description, Open Graph, Twitter Card und JSON-LD im <head>
 *   (die Seiten werden vom Scala.js-Client erzeugt, Crawler und Link-Vorschauen sehen nur das Server-HTML)
 * - noindex und Ausschluss aus der Sitemap für Seiten ohne Nutzen für Suchende
 * - keine Benutzer-Sitemap, keine Autorenarchive und keine öffentliche Benutzerliste (verraten Anmeldenamen)

 * Titel und Beschreibung der Startseite stammen aus config/app.json ("hero", "brand").
 */

/** Seiten-Slugs, die nicht in Suchmaschinen erscheinen sollen (inkl. Unterseiten). */
const WEBAPP_SEO_NOINDEX_SLUGS = ['login', 'registration', 'registrierung', 'verification'];

/**
 * Liefert die IDs aller Seiten, die nicht indexiert werden sollen (WEBAPP_SEO_NOINDEX_SLUGS samt Unterseiten).
 *
 * @return int[] Seiten-IDs.
 */
function webapp_seo_noindex_ids() {
    static $ids = null;
    if ($ids !== null) {
        return $ids;
    }
    $ids = [];
    foreach (WEBAPP_SEO_NOINDEX_SLUGS as $slug) {
        $page = get_page_by_path($slug, OBJECT, 'page');
        if ($page) {
            $ids[] = $page->ID;
            foreach (get_pages(['child_of' => $page->ID, 'post_status' => 'publish,private']) as $child) {
                $ids[] = $child->ID;
            }
        }
    }
    return $ids = array_values(array_unique($ids));
}

/**
 * Seitentitel: Startseite mit Kurzbeschreibung, sonst WordPress-Standard ("Seite – Website").
 *
 * @param array $parts Titelbestandteile von WordPress.
 * @return array Angepasste Titelbestandteile.
 */
function webapp_seo_title_parts($parts) {
    if (is_front_page() || webapp_is_en_home()) {
        $parts = ['title' => webapp_config_text(webapp_config()['hero'][0] ?? []), 'site' => get_bloginfo('name')];
    }
    return $parts;
}

/**
 * Prüft, ob die aktuelle Seite nicht indexiert werden soll (Slug-Liste).
 *
 * @return bool True für noindex-Seiten.
 */
function webapp_seo_is_noindex() {
    if (!is_singular()) {
        return false;
    }
    $post = get_queried_object();
    return in_array($post->ID, webapp_seo_noindex_ids(), true);
}

/**
 * Gibt Meta-Description, Open Graph, Twitter Card und JSON-LD für die aktuelle Seite aus.
 *
 * @return void
 */
function webapp_seo_head() {
    if (is_404() || webapp_seo_is_noindex()) {
        return;
    }
    $site  = get_bloginfo('name');
    $url   = is_singular() ? get_permalink() : home_url('/');
    $title = wp_get_document_title();
    $type  = 'website';
    $lang  = webapp_lang();
    $desc  = webapp_config_text(webapp_config()['hero'][2] ?? []);
    $jsonld = [];

    $static = webapp_static_current();
    if ($static) {
        // Statische Seite (includes/static-pages.php): WordPress gibt dafür keine Canonical-URL aus
        $url  = webapp_static_url($static['de'], $static['lang']);
        $desc = $static['desc'];
        echo '<link rel="canonical" href="' . esc_url($url) . '" />' . "\n";
    } elseif (is_front_page() || webapp_is_en_home()) {
        if (webapp_is_en_home()) {
            // Englische Startseite (includes/lang.php): kein WordPress-Beitrag, daher eigene Canonical-URL
            $url = home_url('/en/');
            echo '<link rel="canonical" href="' . esc_url($url) . '" />' . "\n";
        }
        $sameAs = array_values(array_filter([getenv('SOCIAL_FACEBOOK_URL'), getenv('SOCIAL_X_URL'), getenv('SOCIAL_INSTAGRAM_URL')]));
        $jsonld[] = [
            '@context'            => 'https://schema.org',
            '@type'               => 'WebApplication',
            'name'                => webapp_config_text(webapp_config()['brand'] ?? []),
            'url'                 => $url,
            'description'         => $desc,
            'applicationCategory' => 'WebApplication',
            'operatingSystem'     => 'Web',
            'inLanguage'          => ['de', 'en'],
            'offers'              => ['@type' => 'Offer', 'price' => '0', 'priceCurrency' => 'EUR'],
        ];
        $jsonld[] = array_filter([
            '@context' => 'https://schema.org',
            '@type'    => 'Organization',
            'name'     => $site,
            'url'      => home_url('/'),
            'logo'     => plugins_url('img/logo.png', WEBAPP_PLUGIN_PATH . 'webapp.php'),
            'sameAs'   => $sameAs ?: null,
        ]);
    }
    $desc  = wp_strip_all_tags($desc);
    $desc  = mb_strlen($desc) > 160 ? mb_substr($desc, 0, 157) . '…' : $desc;
    $image = plugins_url('img/og-image.jpg', WEBAPP_PLUGIN_PATH . 'webapp.php');

    $meta = [
        ['name', 'description', $desc],
        ['property', 'og:type', $type],
        ['property', 'og:site_name', $site],
        ['property', 'og:locale', $lang === 'en' ? 'en_GB' : 'de_DE'],
        ['property', 'og:title', $title],
        ['property', 'og:description', $desc],
        ['property', 'og:url', $url],
        ['property', 'og:image', $image],
        ['property', 'og:image:width', '1200'],
        ['property', 'og:image:height', '630'],
        ['name', 'twitter:card', 'summary_large_image'],
        ['name', 'twitter:title', $title],
        ['name', 'twitter:description', $desc],
        ['name', 'twitter:image', $image],
    ];
    foreach ($meta as [$attr, $key, $value]) {
        echo '<meta ' . $attr . '="' . esc_attr($key) . '" content="' . esc_attr($value) . '" />' . "\n";
    }
    foreach ($jsonld as $data) {
        echo '<script type="application/ld+json">' . wp_json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) . '</script>' . "\n";
    }
}

/**
 * Setzt noindex für Seiten ohne Nutzen für Suchende (Anmeldung, Registrierung, Bestätigung).
 *
 * @param array $robots Robots-Direktiven von WordPress.
 * @return array Angepasste Direktiven.
 */
function webapp_seo_robots($robots) {
    if (webapp_seo_is_noindex()) {
        $robots['noindex'] = true;
        $robots['follow']  = true;
    }
    return $robots;
}

/**
 * Nimmt die noindex-Seiten aus der Sitemap.
 *
 * @param array  $args      WP_Query-Argumente der Sitemap.
 * @param string $post_type Post-Typ der Sitemap.
 * @return array Angepasste Argumente.
 */
function webapp_seo_sitemap_query($args, $post_type) {
    if ($post_type === 'page') {
        $args['post__not_in'] = array_merge($args['post__not_in'] ?? [], webapp_seo_noindex_ids());
    }
    return $args;
}

/**
 * Leitet Autorenarchive (auch ?author=N) auf die Startseite um, damit keine Anmeldenamen sichtbar werden.
 *
 * @return void
 */
function webapp_seo_redirect_author() {
    if (is_author()) {
        wp_safe_redirect(home_url('/'), 301);
        exit;
    }
}

/**
 * Entfernt die öffentliche Benutzerliste der REST-API (/wp/v2/users) für nicht angemeldete Besucher.
 *
 * @param array $endpoints Registrierte REST-Endpunkte.
 * @return array Endpunkte ohne Benutzer-Routen.
 */
function webapp_seo_rest_endpoints($endpoints) {
    if (!is_user_logged_in()) {
        unset($endpoints['/wp/v2/users'], $endpoints['/wp/v2/users/(?P<id>[\d]+)']);
    }
    return $endpoints;
}

add_filter('document_title_parts', 'webapp_seo_title_parts');
add_action('wp_head', 'webapp_seo_head', 2);
add_filter('wp_robots', 'webapp_seo_robots');
add_filter('wp_sitemaps_posts_query_args', 'webapp_seo_sitemap_query', 10, 2);
add_filter('wp_sitemaps_add_provider', fn($provider, $name) => $name === 'users' ? false : $provider, 10, 2);
add_action('template_redirect', 'webapp_seo_redirect_author', 1); // vor redirect_canonical, das ?author=N sonst auf /author/<name>/ umleitet
add_filter('rest_endpoints', 'webapp_seo_rest_endpoints');
