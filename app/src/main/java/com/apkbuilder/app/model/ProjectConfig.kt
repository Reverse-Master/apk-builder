package com.apkbuilder.app.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * The user-editable configuration for a single project. This is what the
 * drag-and-drop editor mutates and what the [com.apkbuilder.app.builder.ApkAssembler]
 * injects into the chosen template at build time.
 *
 * It is plain data so it can be serialised to JSON, persisted between sessions
 * and dropped straight into the final APK as `assets/config.json`, where the
 * template's runtime reads it back on startup.
 */
data class ProjectConfig(
    var templateId: String,
    var appName: String = "My App",
    var packageName: String = "com.myapp.app",
    var versionName: String = "1.0",
    var versionCode: Int = 1,

    // Visual customisation -----------------------------------------------------
    var primaryColor: Int = 0xFF3D5AFE.toInt(),
    var accentColor: Int = 0xFF00BFA5.toInt(),
    var backgroundColor: Int = 0xFFFFFFFF.toInt(),

    var headerTitle: String = "Welcome",
    var showTopBar: Boolean = true,
    var showBottomBar: Boolean = true,

    var windowWidthDp: Int = 0,   // 0 = match parent
    var windowHeightDp: Int = 0,

    // Media - paths point to files copied into the project's working directory.
    var iconPath: String? = null,
    var splashImagePath: String? = null,
    var audioPath: String? = null,

    // Tabs / sections - each entry becomes a screen in tab/news templates.
    var tabs: MutableList<TabSpec> = mutableListOf(
        TabSpec("Home", "home"),
        TabSpec("Browse", "search"),
        TabSpec("Profile", "person")
    )
) {
    fun toJson(): String = JSONObject().apply {
        put("templateId", templateId)
        put("appName", appName)
        put("packageName", packageName)
        put("versionName", versionName)
        put("versionCode", versionCode)
        put("primaryColor", primaryColor)
        put("accentColor", accentColor)
        put("backgroundColor", backgroundColor)
        put("headerTitle", headerTitle)
        put("showTopBar", showTopBar)
        put("showBottomBar", showBottomBar)
        put("windowWidthDp", windowWidthDp)
        put("windowHeightDp", windowHeightDp)
        put("tabs", JSONArray().apply {
            tabs.forEach { put(JSONObject().put("title", it.title).put("icon", it.icon)) }
        })
    }.toString(2)

    companion object {
        fun fromJson(json: String): ProjectConfig {
            val o = JSONObject(json)
            val tabs = mutableListOf<TabSpec>()
            val arr = o.optJSONArray("tabs") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                tabs += TabSpec(t.getString("title"), t.getString("icon"))
            }
            return ProjectConfig(
                templateId = o.getString("templateId"),
                appName = o.optString("appName", "My App"),
                packageName = o.optString("packageName", "com.myapp.app"),
                versionName = o.optString("versionName", "1.0"),
                versionCode = o.optInt("versionCode", 1),
                primaryColor = o.optInt("primaryColor", 0xFF3D5AFE.toInt()),
                accentColor = o.optInt("accentColor", 0xFF00BFA5.toInt()),
                backgroundColor = o.optInt("backgroundColor", 0xFFFFFFFF.toInt()),
                headerTitle = o.optString("headerTitle", "Welcome"),
                showTopBar = o.optBoolean("showTopBar", true),
                showBottomBar = o.optBoolean("showBottomBar", true),
                windowWidthDp = o.optInt("windowWidthDp", 0),
                windowHeightDp = o.optInt("windowHeightDp", 0),
                tabs = if (tabs.isEmpty()) mutableListOf(TabSpec("Home", "home")) else tabs
            )
        }
    }
}

data class TabSpec(var title: String, var icon: String)

/** ABI options exposed to the user on the build screen. */
enum class AbiTarget(val label: String, val abis: List<String>) {
    ABI_32("32-bit", listOf("armeabi-v7a", "x86")),
    ABI_64("64-bit", listOf("arm64-v8a", "x86_64")),
    UNIVERSAL("32 + 64-bit", listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
}
