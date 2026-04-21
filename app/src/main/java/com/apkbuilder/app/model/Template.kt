package com.apkbuilder.app.model

/**
 * Describes a ready-to-use app template that the user can pick from the gallery.
 *
 * Each template is shipped inside `assets/templates/<id>/` and consists of:
 *  - `template.apk`        - pre-compiled APK skeleton with placeholder assets
 *  - `manifest.json`       - describes which assets / colors / strings can be customised
 *  - `preview.png`         - thumbnail shown in the gallery
 */
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val category: String
) {
    companion object {
        /** Built-in template catalogue. New templates can be added here without code changes elsewhere. */
        val BUILT_IN = listOf(
            Template("blank",    "Blank App",        "An empty single-screen app, perfect to start from.", "Starter"),
            Template("tabs",     "Tabbed App",       "Bottom-tab navigation with 3 customisable screens.",  "Navigation"),
            Template("webview",  "Website Wrapper",  "Wraps any website (or your own HTML) in a native app.", "Utility"),
            Template("gallery",  "Photo Gallery",    "Grid of photos with full-screen viewer.",             "Media"),
            Template("audio",    "Audio Player",     "Plays a list of bundled audio tracks.",               "Media"),
            Template("news",     "News / Blog",      "Headline list + article detail screens.",             "Content")
        )
    }
}
