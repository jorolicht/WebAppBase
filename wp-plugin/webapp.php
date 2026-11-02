<?php
/**
 * Plugin Name: WebAppBase
 * Plugin URI: https://github.com/jorolicht/WebAppBase
 * Description: Vorlage für Web-Anwendungen auf Basis von WordPress mit Scala.js-Client und Play-Server
 * Version: 0.1.0
 * Requires at least: 6.0
 * Requires PHP: 8.0
 * Author: Robert Lichtenegger
 * License: GPLv2 or later
 * License URI: https://www.gnu.org/licenses/gpl-2.0.html
 * Text Domain: webapp
 */

if (!defined('ABSPATH')) {
    exit; // Sicherheitsprüfung
}

// SemVer MAJOR.MINOR.PATCH Versionskonstanten
define('WEBAPP_VERSION', '0.1.0');
define('WEBAPP_DB_VERSION', '0.1.0');

// Define Plugin-Path
define('WEBAPP_PLUGIN_PATH', plugin_dir_path(__FILE__));

/**
 * Plugin activation hook.
 *
 * Initialisiert Schema- und Versions-Optionen bei der Aktivierung des Plugins.
 * Führt bei Bedarf Upgrades aus.
 *
 * @return void
 */
function webapp_activate() {
    $installed_db_ver = get_option('webapp_db_version');
    if ($installed_db_ver === false) {
        add_option('webapp_db_version', WEBAPP_DB_VERSION);
    } elseif (version_compare($installed_db_ver, WEBAPP_DB_VERSION, '<')) {
        webapp_upgrade_db($installed_db_ver, WEBAPP_DB_VERSION);
        update_option('webapp_db_version', WEBAPP_DB_VERSION);
    }
    update_option('webapp_version', WEBAPP_VERSION);
}
register_activation_hook(__FILE__, 'webapp_activate');

/**
 * Führt Versionsprüfungen und eventuelle DB-Migrationen beim Laden der Plugins durch.
 *
 * @return void
 */
function webapp_check_version() {
    $installed_db_ver = get_option('webapp_db_version');
    if ($installed_db_ver && version_compare($installed_db_ver, WEBAPP_DB_VERSION, '<')) {
        webapp_upgrade_db($installed_db_ver, WEBAPP_DB_VERSION);
        update_option('webapp_db_version', WEBAPP_DB_VERSION);
    } elseif ($installed_db_ver === false) {
        // Falls Option noch fehlt (z. B. Plugin wurde vor Hook-Implementierung aktiviert)
        update_option('webapp_db_version', WEBAPP_DB_VERSION);
    }
    if (get_option('webapp_version') !== WEBAPP_VERSION) {
        update_option('webapp_version', WEBAPP_VERSION);
    }
}
add_action('plugins_loaded', 'webapp_check_version');

/**
 * Führt schrittweise Upgrades / Migrationen der Datenbank durch.
 *
 * @param string $from_version Bisherige Version.
 * @param string $to_version   Neue Zielversion.
 * @return void
 */
function webapp_upgrade_db($from_version, $to_version) {
    error_log(sprintf('WebApp DB upgraded from %s to %s', $from_version, $to_version));
}


// Update-sichere mehrsprachige Dialoge für WP-Members.
add_filter( 'wpmem_default_msgs', 'my_multilingual_registration_msgs' );

function my_multilingual_registration_msgs( $msgs ) {
    // Sprache ermitteln (Polylang / WPML / WordPress Default)
    if ( function_exists('pll_current_language') ) {
        $lang = pll_current_language();
    } elseif ( defined('ICL_LANGUAGE_CODE') ) {
        $lang = ICL_LANGUAGE_CODE;
    } else {
        $lang = substr(get_locale(), 0, 2); // Nimmt z.B. "de" aus "de_DE"
    }

    // Sprachweichen
    switch ( $lang ) {
        case 'en':
            $msgs['register_success'] = 'Congratulations! Your registration was successful.<br /><br />You can now log in using your Passkey or the credentials sent to you.';
            $msgs['msg_username_exists'] = 'Sorry, that username is already taken.';
            break;

        case 'de':
        default:
            $msgs['register_success'] = 'Herzlichen Glückwunsch! Deine Registrierung war erfolgreich.<br /><br />Du kannst dich jetzt mit deinem Passkey oder deinen Zugangsdaten anmelden.';
            $msgs['msg_username_exists'] = 'Dieser Benutzername ist leider schon vergeben.';
            break;
    }

    return $msgs;
}


// Load required files
require_once WEBAPP_PLUGIN_PATH . 'includes/app-config.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/user.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/settings.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/api/user.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/api/webauthn.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/seo.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/home-static.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/static-pages.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/lang.php';
require_once WEBAPP_PLUGIN_PATH . 'includes/helpers.php';


/**
 * Renders the shortcode for the "webapp" application/plugin.
 *
 * This function retrieves the necessary URLs and a nonce for secure communication
 * and then generates the HTML output for the application's front-end. It outputs
 * a container for the application and a script tag that initializes the main
 * JavaScript application with the required data.
 *
 * The shortcode `[webapp]` embeds the application; `[webapp page="login"]`
 * (login, register, verify) opens a specific page directly. Static pages
 * (includes/static-pages.php) are opened via data-page="static:<file>".
 *
 * @param array $atts Shortcode attributes.
 * @return string The complete HTML output for the shortcode.
 */
function webapp_render($atts) {

    $atts = shortcode_atts( array(
        'page' => '',      // optional page parameter for more specific views (login, register, verify)
    ), $atts );

    $jsPath  = plugin_dir_path(__FILE__) . 'js/main.js';
    $jsVer   = file_exists($jsPath) ? filemtime($jsPath) : WEBAPP_VERSION;
    $jsUrl   = plugins_url('js/main.js', __FILE__) . '?v=' . $jsVer;
    $dataUrl = plugins_url('data/', __FILE__);
    $configUrl = plugins_url('config/app.json', __FILE__);
    $configPath = WEBAPP_PLUGIN_PATH . 'config/app.json';
    $configUrl .= '?v=' . (file_exists($configPath) ? filemtime($configPath) : WEBAPP_VERSION);
    $imgUrl  = plugins_url('img/', __FILE__);
    $playUrl = get_option('webapp_server_url', get_option('webapp_url', ''));
    $pageId  = get_the_ID();
    $homeUrl = home_url();
    $nonce   = wp_create_nonce('wp_rest');

    $post = get_post($pageId);

    // URL Parameter 
    $logLevel  = isset($_GET['logLevel']) ? $_GET['logLevel'] : 'debug';
    $isHome    = is_front_page() || webapp_is_en_home();

    // Statische Seite (/impressum/, /preise/ …, includes/static-pages.php): Client öffnet sie über data-page
    $staticPage = webapp_static_current();
    if ($staticPage) {
        $atts['page'] = 'static:' . $staticPage['page'];
        $pageId = 0;
        $post   = null;
    } elseif ($isHome && !is_front_page()) {
        // Englische Startseite /en/: wie die Startseite behandeln (Seitenzustand)
        $pageId = (int) get_option('page_on_front');
        $post   = get_post($pageId);
    }
    // Sprache aus der URL für Startseite und statische Seiten (includes/lang.php), sonst entscheidet der Client
    $pageLang = ($isHome || $staticPage) ? webapp_lang() : '';

    $canEdit = webapp_user_can_edit_post($pageId);

    $turnstile_key = get_option('cfturnstile_key', getenv('TURNSTILE_SITEKEY') ?: '');
    if (!empty($turnstile_key)) {
         wp_enqueue_script('cloudflare-turnstile', 'https://challenges.cloudflare.com/turnstile/v0/api.js', array(), null, true);
    }

    $output = '<div class="webapp-spa-root alignfull">';
    $output .= '<span id="Main_ParamId" 
        data-version="' . esc_attr(WEBAPP_VERSION) . '" 
        data-page="' . esc_attr($atts['page']) . '" 
        data-dataurl="' . esc_url($dataUrl) . '" 
        data-configurl="' . esc_url($configUrl) . '" 
        data-imgurl="' . esc_url($imgUrl) . '" 
        data-homeurl="' . esc_url($homeUrl) . '" 
        data-playurl="' . esc_url($playUrl) . '" 
        data-nonce="' . esc_attr($nonce) . '" 
        data-pageid="' . esc_attr($pageId) . '" 
        data-turnstilesitekey="' . esc_attr($turnstile_key) . '"
        data-socialfacebook="' . esc_url(getenv('SOCIAL_FACEBOOK_URL') ?: '') . '"
        data-socialx="' . esc_url(getenv('SOCIAL_X_URL') ?: '') . '"
        data-socialinstagram="' . esc_url(getenv('SOCIAL_INSTAGRAM_URL') ?: '') . '"
        data-ishome="' . ($isHome ? '1' : '0') . '"
        data-lang="' . esc_attr($pageLang) . '"
        data-canedit="' . ($canEdit ? '1' : '0') . '"
    ></span>';   
   
    $output .= '<span id="Main_NavbarId"></span>';
    $output .= '<div id="Main_AppWrapper" class="d-flex flex-column min-vh-100">';
    $output .= '   <div id="Main_ContentId" class="flex-grow-1">';
    // Startseite bzw. statische Seiten: Inhalt schon im HTML (SEO), der Client ersetzt ihn beim Start
    if ($staticPage) {
        $homeHtml = webapp_static_html($staticPage);
    } else {
        $homeHtml = ($isHome && empty($atts['page'])) ? webapp_home_static_html() : '';
    }
    $output .= $homeHtml !== '' ? $homeHtml : '      <div class="d-flex mt-5 justify-content-center"><span>Wird geladen...</span></div>';
    $output .= '   </div>';
    $output .= '   <span id="Main_FooterId"></span>';
    $output .= '</div>';
    $output .= '<span id="Footer_ConsoleClickId" data-command=""></span>';
    $output .= '<span id="Main_DynContentId"></span>';
    $output .= '<script type="module">';

    $output .= 'import { startApp } from "' . esc_url($jsUrl) . '";';
    $output .= 'startApp("' . esc_attr(WEBAPP_VERSION) . '", "' . esc_attr($logLevel) . '");';
    $output .= '</script>';
    $output .= '</div>';

    return $output;
}
add_shortcode('webapp', 'webapp_render');

/**
 * Konfiguriert PHPMailer für MailCatcher im Docker-Netzwerk.
 * Nur in der Dev-Umgebung (WEBAPP_MAILCATCHER in wp-config.php, gesetzt über server/docker/dev/docker-compose.yml);
 * sonst bleibt der Mailversand (z. B. WP Mail SMTP) unverändert.
 *
 * @param PHPMailer $phpmailer Das PHPMailer-Instanz-Objekt.
 */
if (defined('WEBAPP_MAILCATCHER') && WEBAPP_MAILCATCHER) {
    add_action('phpmailer_init', function($phpmailer) {
        $phpmailer->isSMTP();
        $phpmailer->Host = 'mailcatcher'; // Hostname im Docker-Netzwerk
        $phpmailer->Port = 1025;
        $phpmailer->SMTPAuth = false;
        $phpmailer->SMTPSecure = '';
        $phpmailer->SMTPAutoTLS = false;
    }, 99999);
}

/**
 * Verhindert, dass WordPress den [webapp] Shortcode in <p>-Tags einschließt,
 * da dieser Block-Elemente (div) generiert, was zu invalidem HTML (stray </p>) führt.
 * Dieser Filter läuft bei Priorität 12 (nach do_shortcode), um die generierten Container sauber zu säubern.
 */
function webapp_unautop_shortcode($content) {
    if (false === strpos($content, 'webapp-spa-root')) {
        return $content;
    }
    // Entfernt umgebende <p>-Tags um die webapp-spa-root Division
    $content = preg_replace('/<p>\s*(<div class="webapp-spa-root".*?<\/div>)\s*<\/p>/is', '$1', $content);
    return $content;
}
add_filter('the_content', 'webapp_unautop_shortcode', 12);


/**
 * Das leere Template für alle Frontend-Seiten erzwingen: es erscheinen keine WordPress-Header/-Footer,
 * nur die vom Scala.js-Client erzeugten.
 *
 * @param string $template Pfad des von WordPress gewählten Templates (wird nicht verwendet).
 * @return void Beendet die Anfrage nach der Ausgabe.
 */
function webapp_force_blank_template( $template ) {
    webapp_render_direct_blank_page();
    exit;
}
add_filter( 'template_include', 'webapp_force_blank_template', 999 );

/**
 * Blendet die WordPress-Admin-Leiste im Frontend aus,
 * außer für WordPress-Administratoren.
 *
 * @param bool $show Ob WordPress die Admin-Leiste anzeigen würde.
 * @return bool True, wenn die Admin-Leiste angezeigt werden soll.
 */
function webapp_admin_bar_only_for_admins( $show ) {
    if ( ! in_array( 'administrator', (array) wp_get_current_user()->roles, true ) ) {
        return false;
    }
    return $show;
}
add_filter( 'show_admin_bar', 'webapp_admin_bar_only_for_admins' );

/**
 * 3. Das nackte HTML-Gerüst inklusive App-Root ausgeben
 */
function webapp_render_direct_blank_page() {
    ?>
    <!DOCTYPE html>
    <html <?php language_attributes(); ?>>
    <head>
        <meta charset="<?php bloginfo( 'charset' ); ?>">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <?php 
        // Lädt Header-Metas und CSS-Dateien
        wp_head(); 
        ?>
        <style>
            html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                overflow-x: hidden;
            }
        </style>
    </head>
    <body <?php body_class(); ?>>
        <?php 
        echo webapp_render( webapp_shortcode_atts_of_current_post() );
        wp_footer(); 
        ?>
    </body>
    </html>
    <?php
}


/**
 * Liest die Attribute des ersten [webapp]-Shortcodes im aktuell angezeigten Post
 * (z. B. page="login"), damit sie auch auf der leeren Seite gelten.
 *
 * @return array Shortcode-Attribute oder leeres Array, wenn kein Shortcode vorhanden ist.
 */
function webapp_shortcode_atts_of_current_post() {
    $post = is_singular() ? get_queried_object() : null;
    if ( $post instanceof WP_Post && preg_match( '/' . get_shortcode_regex( array( 'webapp' ) ) . '/', $post->post_content, $m ) ) {
        $atts = shortcode_parse_atts( $m[3] );
        return is_array( $atts ) ? $atts : array();
    }
    return array();
}


/**
 * Enqueues scripts and styles for the plugin's front-end.
 *
 * This function is hooked to the 'wp_enqueue_scripts' action. It registers
 * and enqueues several external and local assets to ensure the plugin's
 * functionality and styling are available on the front-end.
 *
 * It includes:
 * - Bootstrap 5 CSS and JavaScript from a CDN.
 * - Bootstrap Icons CSS from a CDN.
 * - A custom CSS file located in the plugin's 'css' directory.
 * - A custom JavaScript file located in the plugin's 'js' directory.
 *
 * Note: The scripts are configured to be loaded in the document head (the 'false'
 * parameter), not the footer.
 *
 * @return void
 */
function js_enqueue_scripts_styles() {
    // Basis-Stylesheet mit Cache-Busting (filemtime oder WEBAPP_VERSION)
    $style_css_path = WEBAPP_PLUGIN_PATH . 'css/style.css';
    $style_ver      = file_exists($style_css_path) ? filemtime($style_css_path) : WEBAPP_VERSION;
    wp_enqueue_style( 
        'mein-plugin-basis-style', 
        plugin_dir_url( __FILE__ ) . 'css/style.css', 
        array(), 
        $style_ver 
    );    

    // Externe Bibliotheken
    wp_enqueue_style( 'bootstrap', 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css', array(), '5.3.3' );
    wp_enqueue_style( 'bootstrap-icons', 'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css', array(), '1.11.3' );
    wp_enqueue_script( 'bootstrap', 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js', array(), '5.3.3', false );
    // Haupt-Stylesheet mit Cache-Busting
    $main_css_path = WEBAPP_PLUGIN_PATH . 'css/main.css';
    $main_ver      = file_exists($main_css_path) ? filemtime($main_css_path) : WEBAPP_VERSION;
    wp_enqueue_style( 'webapp_style', plugin_dir_url(__FILE__) . 'css/main.css', array(), $main_ver);
}
add_action('wp_enqueue_scripts', 'js_enqueue_scripts_styles');


/**
 * Allows the uploading of JSON files in WordPress.
 *
 * This function adds the 'application/json' MIME type to the list of allowed
 * file types for uploads. By default, WordPress does not permit the upload
 * of .json files for security reasons. This filter is necessary for plugins or themes
 * that need to handle JSON data via the media uploader.
 *
 * @param array $mimes An associative array of allowed MIME types and their extensions.
 * @return array The updated associative array of allowed MIME types.
 */
function allow_json_mime_type( $mimes ) {
    $mimes['json'] = 'application/json';
    return $mimes;
}
add_filter( 'upload_mimes', 'allow_json_mime_type' );

/**
 * Enable 'lang' parameter for Polylang in WP REST API (Free version workaround).
 * Allows fetching pages/posts by specific language using ?lang=XX.
 */
function webapp_polylang_rest_query_filter($args, $request) {
    $lang = $request->get_param('lang');
    if (!empty($lang)) {
        $args['lang'] = $lang;
    }
    return $args;
}
add_filter('rest_post_query', 'webapp_polylang_rest_query_filter', 10, 2);
add_filter('rest_page_query', 'webapp_polylang_rest_query_filter', 10, 2);




