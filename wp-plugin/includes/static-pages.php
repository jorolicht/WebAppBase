<?php

/**
 * Eigene URLs für die statischen Seiten, z. B. /impressum/ oder /preise/,
 * englisch unter /en/imprint/ bzw. /en/pricing/ (Sprache siehe includes/lang.php).
 *
 * Eine Rewrite-Regel bildet die Slugs auf die Query-Variable webapp_static ab, ohne WordPress-Seiten anzulegen.
 * Der Inhalt kommt aus pages/<datei>.html (kopiert aus site/content) und steht schon im Server-HTML (SEO);
 * der Client öffnet die Seite beim Start über data-page="static:<seite>" (MainView) und ersetzt den Inhalt.
 * Die Seiten sind in config/app.json ("staticPages") festgelegt; der Client liest dieselbe Datei.
 */

/**
 * Statische Seiten aus config/app.json ("staticPages"), indiziert nach deutschem Slug:
 * Datei in pages/ (ohne .html; englisch mit Suffix _en), Seitenname im Client (MainView-Parameter),
 * Titel und Beschreibung sowie englischer Slug (/en/<slug>/), Titel und Beschreibung.
 *
 * @return array<string,array> Deutscher Slug => Seite.
 */
function webapp_static_pages() {
    static $pages = null;
    if ($pages === null) {
        $pages = [];
        foreach (webapp_config()['staticPages'] ?? [] as $sp) {
            $pages[$sp['slug']['de']] = [
                'file'  => $sp['file'],
                'page'  => $sp['file'],
                'title' => webapp_config_text($sp['title'], 'de'),
                'desc'  => webapp_config_text($sp['desc'] ?? [], 'de'),
                'en'    => [
                    'slug'  => $sp['slug']['en'] ?: $sp['slug']['de'],
                    'title' => webapp_config_text($sp['title'], 'en'),
                    'desc'  => webapp_config_text($sp['desc'] ?? [], 'en'),
                ],
            ];
        }
    }
    return $pages;
}

/**
 * Liefert die statische Seite der aktuellen Anfrage in der Sprache der URL.
 *
 * @return array|null Eintrag aus webapp_static_pages() mit Titel/Beschreibung/Datei der Sprache, ergänzt um
 *                    'slug' (Slug der URL), 'de' (deutscher Slug) und 'lang', oder null.
 */
function webapp_static_current() {
    $slug = get_query_var('webapp_static');
    if (!$slug) {
        return null;
    }
    if (webapp_lang() === 'en') {
        foreach (webapp_static_pages() as $de => $page) {
            if ($page['en']['slug'] === $slug) {
                return array_merge($page, $page['en'], ['de' => $de, 'lang' => 'en', 'file' => $page['file'] . '_en']);
            }
        }
        return null;
    }
    return isset(webapp_static_pages()[$slug]) ? webapp_static_pages()[$slug] + ['slug' => $slug, 'de' => $slug, 'lang' => 'de'] : null;
}

/**
 * URL einer statischen Seite in einer Sprache.
 *
 * @param string $de   Deutscher Slug (Schlüssel in webapp_static_pages()).
 * @param string $lang 'de' oder 'en'.
 * @return string Absolute URL, z. B. https://…/impressum/ bzw. https://…/en/imprint/.
 */
function webapp_static_url($de, $lang) {
    return $lang === 'en' ? home_url('/en/' . webapp_static_pages()[$de]['en']['slug'] . '/') : home_url("/$de/");
}

/**
 * Registriert die Rewrite-Regel und schreibt die Regeln neu, wenn sich die Slug-Liste geändert hat
 * (gemerkt in Option webapp_static_rules; flush_rewrite_rules ist teuer und läuft daher nur einmal).
 *
 * @return void
 */
function webapp_static_rewrite() {
    $slugs   = implode('|', array_keys(webapp_static_pages()));
    $enSlugs = implode('|', array_map(fn($page) => $page['en']['slug'], webapp_static_pages()));
    add_rewrite_rule("^($slugs)/?$", 'index.php?webapp_static=$matches[1]', 'top');
    add_rewrite_rule("^en/($enSlugs)/?$", 'index.php?webapp_lang=en&webapp_static=$matches[1]', 'top');
    if (get_option('webapp_static_rules') !== "$slugs;$enSlugs") {
        flush_rewrite_rules(false);
        update_option('webapp_static_rules', "$slugs;$enSlugs");
    }
}

/**
 * Verhindert die 404-Behandlung für statische Seiten (kein WordPress-Beitrag passt zur Anfrage).
 *
 * @param bool $preempt Bisheriger Wert.
 * @return bool True für statische Seiten.
 */
function webapp_static_pre_404($preempt) {
    return webapp_static_current() ? true : $preempt;
}

/**
 * Erzeugt das HTML einer statischen Seite für #Main_ContentId, im Aufbau wie MainView/PageContent.scala.html.
 *
 * @param array $page Eintrag aus webapp_static_current().
 * @return string HTML oder leerer String, wenn die Datei fehlt.
 */
function webapp_static_html($page) {
    $file = WEBAPP_PLUGIN_PATH . 'pages/' . $page['file'] . '.html';
    if (!file_exists($file)) {
        return '';
    }
    return '<div class="container mt-4 mb-5"><div class="card shadow-lg border-0 overflow-hidden">'
         . '<div class="card-header bg-white border-0 pt-4 px-md-5"><h1 class="display-5 fw-bold text-primary mb-0">' . esc_html($page['title']) . '</h1></div>'
         . '<div class="card-body p-md-5 pt-3"><div class="wp-content-body lh-lg">' . file_get_contents($file) . '</div></div>'
         . '</div></div>';
}

/**
 * Seitentitel der statischen Seiten ("Impressum – Website").
 *
 * @param array $parts Titelbestandteile von WordPress.
 * @return array Angepasste Titelbestandteile.
 */
function webapp_static_title_parts($parts) {
    $page = webapp_static_current();
    return $page ? ['title' => $page['title'], 'site' => get_bloginfo('name')] : $parts;
}

/**
 * Registriert den Sitemap-Provider für die statischen Seiten (wp-sitemap-webappstatic-1.xml).
 *
 * @return void
 */
function webapp_static_sitemap() {
    if (!class_exists('WP_Sitemaps_Provider')) {
        return;
    }
    /** Sitemap der statischen Seiten; lastmod ist das Änderungsdatum der Datei. */
    $provider = new class extends WP_Sitemaps_Provider {
        public function __construct() {
            $this->name        = 'webappstatic';
            $this->object_type = 'webappstatic';
        }
        public function get_url_list($page_num, $object_subtype = '') {
            $list = [];
            foreach (webapp_static_pages() as $slug => $page) {
                foreach (['de' => '', 'en' => '_en'] as $lang => $suffix) {
                    $file  = WEBAPP_PLUGIN_PATH . 'pages/' . $page['file'] . $suffix . '.html';
                    $entry = ['loc' => webapp_static_url($slug, $lang)];
                    if (file_exists($file)) {
                        $entry['lastmod'] = gmdate('c', filemtime($file));
                    }
                    $list[] = $entry;
                }
            }
            return $list;
        }
        public function get_max_num_pages($object_subtype = '') {
            return 1;
        }
    };
    wp_register_sitemap_provider('webappstatic', $provider);
}

add_action('init', 'webapp_static_rewrite');
add_filter('query_vars', fn($vars) => array_merge($vars, ['webapp_static']));
add_filter('pre_handle_404', 'webapp_static_pre_404');
add_filter('redirect_canonical', fn($redirect) => webapp_static_current() ? false : $redirect);
add_filter('document_title_parts', 'webapp_static_title_parts', 20);
add_action('init', 'webapp_static_sitemap');
