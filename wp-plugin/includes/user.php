<?php

/**
 * Helper to check if a user (or current user) is an Administrator or Editor.
 *
 * @param WP_User|null $user Optional. The user object. Defaults to current logged-in user.
 * @return bool True if Administrator or Editor, false otherwise.
 */
function webapp_is_admin_or_editor( $user = null ) {
    if ( ! $user || ! ( $user instanceof WP_User ) ) {
        $user = wp_get_current_user();
    }
    if ( ! $user || ! $user->exists() ) {
        return false;
    }
    $roles = (array) $user->roles;
    return user_can( $user, 'manage_options' ) || 
           user_can( $user, 'edit_others_posts' ) || 
           in_array( 'administrator', $roles, true ) || 
           in_array( 'editor', $roles, true );
}

/**
 * Prüft, ob der aktuelle Benutzer Schreibrechte auf eine Seite hat (WordPress-Recht 'edit_post').
 *
 * @param int $post_id Die WordPress Post-ID.
 * @return bool True, wenn der aktuelle Benutzer den Post bearbeiten darf.
 */
function webapp_user_can_edit_post( $post_id ) {
    $post_id = intval( $post_id );
    if ( $post_id <= 0 || ! is_user_logged_in() ) {
        return false;
    }
    return current_user_can( 'edit_post', $post_id );
}

/**
 * REST-Permission-Callback: Schreibrecht auf den im Request (postId bzw. slug) angegebenen Post.
 *
 * @param WP_REST_Request $request Der REST-Request.
 * @return bool True, wenn der aktuelle Benutzer den Post bearbeiten darf.
 */
function webapp_api_can_edit_request_post( WP_REST_Request $request ) {
    return webapp_user_can_edit_post( ApiHelper::getPostId( $request ) );
}
