package com.agon.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore("devbrowser")

class BrowserRepository(context: Context) {

    private val ds = context.applicationContext.dataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val keySettings = stringPreferencesKey("settings")
    private val keyBookmarks = stringPreferencesKey("bookmarks")
    private val keyHistory = stringPreferencesKey("history")
    private val keyDownloads = stringPreferencesKey("downloads")
    private val keyShortcuts = stringPreferencesKey("shortcuts")
    private val keyEngines = stringPreferencesKey("engines")
    private val keyScripts = stringPreferencesKey("scripts")
    private val keyExtensions = stringPreferencesKey("extensions")
    private val keyWebApps = stringPreferencesKey("webapps")
    private val keySites = stringPreferencesKey("sites")
    private val keySaved = stringPreferencesKey("savedpages")

    val settings: Flow<BrowserSettings> = ds.data.map { prefs ->
        decode(prefs[keySettings], BrowserSettings())
    }
    val bookmarks: Flow<List<BookmarkItem>> = listFlow(keyBookmarks, emptyList())
    val history: Flow<List<HistoryItem>> = listFlow(keyHistory, emptyList())
    val downloads: Flow<List<DownloadRecord>> = listFlow(keyDownloads, emptyList())
    val shortcuts: Flow<List<Shortcut>> = listFlow(keyShortcuts, DefaultShortcuts.list)
    val engines: Flow<List<SearchEngine>> = listFlow(keyEngines, SearchEngines.defaults)
    val scripts: Flow<List<UserScript>> = listFlow(keyScripts, DefaultData.scripts)
    val extensions: Flow<List<ExtensionRecord>> = listFlow(keyExtensions, DefaultData.extensions)
    val webApps: Flow<List<WebAppItem>> = listFlow(keyWebApps, DefaultData.webApps)
    val siteConfigs: Flow<List<SiteConfig>> = listFlow(keySites, emptyList())
    val savedPages: Flow<List<SavedPage>> = listFlow(keySaved, emptyList())

    private inline fun <reified T> listFlow(
        key: Preferences.Key<String>,
        default: List<T>,
    ): Flow<List<T>> = ds.data.map { prefs ->
        val raw = prefs[key] ?: return@map default
        try {
            json.decodeFromString(ListSerializer(serializerOf<T>()), raw)
        } catch (e: Exception) {
            default
        }
    }

    private inline fun <reified T> serializerOf() = kotlinx.serialization.serializer<T>()

    private inline fun <reified T> decode(raw: String?, default: T): T {
        if (raw == null) return default
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: Exception) {
            default
        }
    }

    suspend fun saveSettings(value: BrowserSettings) = put(keySettings, json.encodeToString(value))
    suspend fun saveBookmarks(value: List<BookmarkItem>) = putList(keyBookmarks, value)
    suspend fun saveHistory(value: List<HistoryItem>) = putList(keyHistory, value)
    suspend fun saveDownloads(value: List<DownloadRecord>) = putList(keyDownloads, value)
    suspend fun saveShortcuts(value: List<Shortcut>) = putList(keyShortcuts, value)
    suspend fun saveEngines(value: List<SearchEngine>) = putList(keyEngines, value)
    suspend fun saveScripts(value: List<UserScript>) = putList(keyScripts, value)
    suspend fun saveExtensions(value: List<ExtensionRecord>) = putList(keyExtensions, value)
    suspend fun saveWebApps(value: List<WebAppItem>) = putList(keyWebApps, value)
    suspend fun saveSiteConfigs(value: List<SiteConfig>) = putList(keySites, value)
    suspend fun saveSavedPages(value: List<SavedPage>) = putList(keySaved, value)

    private suspend inline fun <reified T> putList(key: Preferences.Key<String>, value: List<T>) {
        put(key, json.encodeToString(ListSerializer(serializerOf<T>()), value))
    }

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        ds.edit { it[key] = value }
    }
}

object DefaultData {
    val scripts = listOf(
        UserScript(
            id = "script_dark",
            name = "Force Dark Reader",
            matches = "*",
            runAt = "document-end",
            enabled = false,
            code = "document.documentElement.style.filter = 'invert(1) hue-rotate(180deg)';\n" +
                "document.querySelectorAll('img,video,picture').forEach(function(e){\n" +
                "  e.style.filter = 'invert(1) hue-rotate(180deg)';\n" +
                "});",
        ),
        UserScript(
            id = "script_grid",
            name = "Layout Debug Outline",
            matches = "*",
            runAt = "document-end",
            enabled = false,
            code = "document.querySelectorAll('*').forEach(function(e){\n" +
                "  e.style.outline = '1px solid rgba(255,0,128,.35)';\n" +
                "});",
        ),
        UserScript(
            id = "script_unsticky",
            name = "Remove Sticky Headers",
            matches = "*",
            runAt = "document-end",
            enabled = false,
            code = "document.querySelectorAll('*').forEach(function(e){\n" +
                "  var p = getComputedStyle(e).position;\n" +
                "  if (p === 'sticky' || p === 'fixed') e.style.position = 'static';\n" +
                "});",
        ),
    )

    val extensions = listOf(
        ExtensionRecord(
            id = "ext_blocker",
            name = "DevBrowser Content Blocker",
            version = "1.4.0",
            source = "Built-in",
            permissions = listOf("declarativeNetRequest", "host: <all_urls>"),
            compatibility = "Full",
            description = "Native host-level request blocker using EasyList-style rules. Runs in the network layer, not the extension sandbox.",
        ),
        ExtensionRecord(
            id = "ext_userscripts",
            name = "User Script Runtime",
            version = "1.1.0",
            source = "Built-in",
            permissions = listOf("scripting", "storage", "host: matched sites"),
            compatibility = "Full",
            description = "Executes Manifest-V3-style content scripts at document-start / document-end in the page context.",
        ),
    )

    val webApps = listOf(
        WebAppItem("wa_youtube", "YouTube", "https://m.youtube.com", "mobile", true, true),
        WebAppItem("wa_x", "X", "https://x.com", "mobile", true, true),
        WebAppItem("wa_reddit", "Reddit", "https://www.reddit.com", "mobile", true, true),
    )
}
