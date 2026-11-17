package com.agon.app.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.browser.BrowserEngine
import com.agon.app.browser.ConsoleMessage
import com.agon.app.browser.NetworkRecord
import com.agon.app.browser.TabState
import com.agon.app.data.BookmarkItem
import com.agon.app.data.BrowserRepository
import com.agon.app.data.BrowserSettings
import com.agon.app.data.DownloadRecord
import com.agon.app.data.ExtensionRecord
import com.agon.app.data.HistoryItem
import com.agon.app.data.SavedPage
import com.agon.app.data.SearchEngine
import com.agon.app.data.SearchEngines
import com.agon.app.data.Shortcut
import com.agon.app.data.SiteConfig
import com.agon.app.data.UserScript
import com.agon.app.data.WebAppItem
import com.agon.app.data.faviconUrl
import com.agon.app.data.hostOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class BrowserViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BrowserRepository(app)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var recordId = 0L

    // ---------------- persisted state ----------------
    var settings by mutableStateOf(BrowserSettings())
        private set
    val bookmarks = mutableStateListOf<BookmarkItem>()
    val history = mutableStateListOf<HistoryItem>()
    val downloads = mutableStateListOf<DownloadRecord>()
    val shortcuts = mutableStateListOf<Shortcut>()
    val engines = mutableStateListOf<SearchEngine>()
    val scripts = mutableStateListOf<UserScript>()
    val extensions = mutableStateListOf<ExtensionRecord>()
    val webApps = mutableStateListOf<WebAppItem>()
    val siteConfigs = mutableStateListOf<SiteConfig>()
    val savedPages = mutableStateListOf<SavedPage>()

    // ---------------- runtime state ----------------
    val tabs = mutableStateListOf<TabState>()
    var activeTabId by mutableStateOf("")
        private set
    val recentlyClosed = mutableStateListOf<Pair<String, String>>()
    var incognitoMode by mutableStateOf(false)
        private set
    var toastMessage by mutableStateOf<String?>(null)
    var pickedNodePath by mutableStateOf<String?>(null)
    var lastFps by mutableIntStateOf(-1)
    var findMatches by mutableIntStateOf(0)
    var findIndex by mutableIntStateOf(0)
    var fullScreen by mutableStateOf(false)
    var devToolsPanel by mutableIntStateOf(1)
    var toolsTarget by mutableStateOf<String?>(null)
    var loaded by mutableStateOf(false)
        private set

    private var tts: TextToSpeech? = null
    var speaking by mutableStateOf(false)

    val activeTab: TabState?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    val searchEngine: SearchEngine
        get() = engines.firstOrNull { it.id == settings.searchEngineId }
            ?: SearchEngines.defaults.first()

    init {
        viewModelScope.launch {
            settings = repo.settings.first()
            bookmarks.addAll(repo.bookmarks.first())
            history.addAll(repo.history.first())
            downloads.addAll(repo.downloads.first())
            shortcuts.addAll(repo.shortcuts.first())
            engines.addAll(repo.engines.first())
            scripts.addAll(repo.scripts.first())
            extensions.addAll(repo.extensions.first())
            webApps.addAll(repo.webApps.first())
            siteConfigs.addAll(repo.siteConfigs.first())
            savedPages.addAll(repo.savedPages.first())
            loaded = true
            if (tabs.isEmpty()) newTab(null, false)
        }
    }

    // ---------------- settings ----------------
    fun updateSettings(block: (BrowserSettings) -> BrowserSettings) {
        settings = block(settings)
        viewModelScope.launch { repo.saveSettings(settings) }
        applySettingsToTabs()
    }

    private fun applySettingsToTabs() {
        tabs.forEach { tab ->
            tab.webView?.settings?.let { s ->
                s.javaScriptEnabled = settings.javascriptEnabled
                s.loadsImagesAutomatically = settings.imagesEnabled
                s.blockNetworkImage = !settings.imagesEnabled
                s.textZoom = settings.textZoom
                s.userAgentString = userAgentFor(tab)
            }
        }
    }

    fun userAgentFor(tab: TabState): String? {
        val cfg = siteConfigs.firstOrNull { it.host == hostOf(tab.url) }
        val desktop = tab.desktopMode || cfg?.desktop == true ||
            (settings.userAgentMode == "desktop") || settings.desktopByDefault
        return when {
            settings.userAgentMode == "custom" && settings.customUserAgent.isNotBlank() -> settings.customUserAgent
            desktop -> BrowserEngine.DESKTOP_UA
            else -> null
        }
    }

    // ---------------- tabs ----------------
    fun newTab(url: String? = null, incognito: Boolean = incognitoMode): TabState {
        val tab = TabState(UUID.randomUUID().toString(), incognito)
        tabs.add(tab)
        activeTabId = tab.id
        if (!url.isNullOrBlank()) {
            tab.pendingRestoreUrl = url
            tab.isHome = false
            tab.url = url
        }
        return tab
    }

    fun selectTab(id: String) {
        captureThumbnail(activeTab)
        activeTabId = id
        tabs.firstOrNull { it.id == id }?.let {
            it.lastVisited = System.currentTimeMillis()
            if (it.suspended) restoreTab(it)
        }
    }

    fun closeTab(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0) return
        val tab = tabs[idx]
        if (!tab.isHome && tab.url.isNotBlank()) {
            recentlyClosed.add(0, tab.displayTitle to tab.url)
            if (recentlyClosed.size > 25) recentlyClosed.removeAt(recentlyClosed.size - 1)
        }
        tab.webView?.let {
            it.stopLoading()
            it.removeAllViews()
            it.destroy()
        }
        tab.webView = null
        tabs.removeAt(idx)
        if (tabs.isEmpty()) {
            newTab(null, incognitoMode)
        } else if (activeTabId == id) {
            activeTabId = tabs[idx.coerceAtMost(tabs.size - 1)].id
        }
    }

    fun closeAllTabs(incognitoOnly: Boolean = false) {
        val targets = tabs.filter { !incognitoOnly || it.incognito }.map { it.id }
        targets.forEach { closeTab(it) }
    }

    fun duplicateTab(id: String) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        newTab(tab.url.ifBlank { null }, tab.incognito)
    }

    fun moveTab(from: Int, to: Int) {
        if (from !in tabs.indices || to !in tabs.indices) return
        val tab = tabs.removeAt(from)
        tabs.add(to, tab)
    }

    fun setTabGroup(id: String, name: String, color: Int) {
        tabs.firstOrNull { it.id == id }?.let { it.groupName = name; it.groupColor = color }
    }

    fun restoreLastClosed() {
        if (recentlyClosed.isEmpty()) {
            toast("No recently closed tabs")
            return
        }
        val entry = recentlyClosed.removeAt(0)
        newTab(entry.second, incognitoMode)
    }

    fun setIncognito(value: Boolean) {
        incognitoMode = value
        val target = tabs.firstOrNull { it.incognito == value }
        if (target != null) activeTabId = target.id else newTab(null, value)
    }

    fun suspendTab(tab: TabState) {
        if (tab.id == activeTabId || tab.isHome) return
        captureThumbnail(tab)
        tab.suspended = true
        tab.webView?.let {
            it.onPause()
            it.pauseTimers()
        }
    }

    fun restoreTab(tab: TabState) {
        tab.suspended = false
        tab.webView?.let {
            it.onResume()
            it.resumeTimers()
        }
    }

    fun captureThumbnail(tab: TabState?) {
        val wv = tab?.webView ?: return
        try {
            if (wv.width <= 0 || wv.height <= 0) return
            val scale = 0.3f
            val bmp = Bitmap.createBitmap(
                (wv.width * scale).toInt().coerceAtLeast(1),
                (wv.height * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.RGB_565,
            )
            val canvas = Canvas(bmp)
            canvas.scale(scale, scale)
            wv.draw(canvas)
            tab.thumbnail = bmp
        } catch (e: Throwable) {
            // ignore rendering failures
        }
    }

    // ---------------- navigation ----------------
    fun isProbablyUrl(input: String): Boolean {
        val text = input.trim()
        if (text.isEmpty() || text.contains(' ')) return false
        if (text.startsWith("http://") || text.startsWith("https://") ||
            text.startsWith("file://") || text.startsWith("about:") || text.startsWith("data:")
        ) return true
        if (text.startsWith("localhost") || text.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$"))) return true
        val host = text.substringBefore('/').substringBefore('?')
        if (!host.contains('.')) return false
        val tld = host.substringAfterLast('.')
        return tld.length in 2..24 && tld.all { it.isLetter() }
    }

    fun buildUrl(input: String): String {
        val text = input.trim()
        return if (isProbablyUrl(text)) {
            if (text.contains("://")) text else "https://$text"
        } else {
            searchEngine.searchUrl.replace("%s", Uri.encode(text))
        }
    }

    fun navigate(input: String, tab: TabState? = activeTab) {
        val target = tab ?: newTab()
        val url = buildUrl(input)
        target.isHome = false
        target.readerMode = false
        target.url = url
        val wv = target.webView
        if (wv != null) wv.loadUrl(url) else target.pendingRestoreUrl = url
    }

    fun goBack(tab: TabState? = activeTab) {
        val t = tab ?: return
        if (t.webView?.canGoBack() == true) t.webView?.goBack() else goHome(t)
    }

    fun goForward(tab: TabState? = activeTab) {
        tab?.webView?.let { if (it.canGoForward()) it.goForward() }
    }

    fun reload(tab: TabState? = activeTab) {
        tab?.webView?.reload()
    }

    fun stopLoading(tab: TabState? = activeTab) {
        tab?.webView?.stopLoading()
        tab?.loading = false
    }

    fun goHome(tab: TabState? = activeTab) {
        val t = tab ?: return
        t.isHome = true
        t.url = ""
        t.title = "New tab"
        t.progress = 0f
        t.loading = false
        t.webView?.loadUrl("about:blank")
    }

    fun toggleDesktopMode(tab: TabState? = activeTab) {
        val t = tab ?: return
        t.desktopMode = !t.desktopMode
        t.webView?.settings?.apply {
            userAgentString = userAgentFor(t)
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        t.webView?.reload()
        toast(if (t.desktopMode) "Desktop site enabled" else "Mobile site enabled")
    }

    // ---------------- WebView creation ----------------
    fun obtainWebView(context: Context, tab: TabState): WebView {
        tab.webView?.let { return it }
        val wv = WebView(context)
        configure(wv, tab)
        tab.webView = wv
        val pending = tab.pendingRestoreUrl ?: tab.url.takeIf { it.isNotBlank() }
        if (!pending.isNullOrBlank()) {
            tab.pendingRestoreUrl = null
            wv.loadUrl(pending)
        }
        return wv
    }

    private fun configure(wv: WebView, tab: TabState) {
        val cfg = wv.settings
        cfg.javaScriptEnabled = settings.javascriptEnabled
        cfg.domStorageEnabled = true
        cfg.databaseEnabled = true
        cfg.loadsImagesAutomatically = settings.imagesEnabled
        cfg.blockNetworkImage = !settings.imagesEnabled
        cfg.useWideViewPort = true
        cfg.loadWithOverviewMode = true
        cfg.setSupportZoom(true)
        cfg.builtInZoomControls = true
        cfg.displayZoomControls = false
        cfg.javaScriptCanOpenWindowsAutomatically = true
        cfg.mediaPlaybackRequiresUserGesture = false
        cfg.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cfg.textZoom = settings.textZoom
        cfg.cacheMode = if (tab.incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        cfg.userAgentString = userAgentFor(tab)
        cfg.setGeolocationEnabled(false)

        WebView.setWebContentsDebuggingEnabled(settings.remoteDebugging)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, !tab.incognito)

        wv.addJavascriptInterface(DevBridge(tab), BrowserEngine.BRIDGE)
        wv.isVerticalScrollBarEnabled = true

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http")) return false
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(intent)
                    true
                } catch (e: Exception) {
                    toast("No app can open this link")
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                tab.loading = true
                tab.crashed = false
                tab.progress = 0.05f
                if (url != null && url != "about:blank") {
                    tab.url = url
                    tab.isHome = false
                    tab.secure = url.startsWith("https://")
                }
                injectRuntime(view)
                runUserScripts(view, "document-start")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                tab.loading = false
                tab.progress = 1f
                tab.canGoBack = view?.canGoBack() == true
                tab.canGoForward = view?.canGoForward() == true
                if (url != null && url != "about:blank") {
                    tab.url = url
                    tab.title = view?.title ?: url
                    recordHistory(tab)
                }
                injectRuntime(view)
                runUserScripts(view, "document-end")
                view?.evaluateJavascript("window.__db_report_resources && window.__db_report_resources();", null)
                if (!settings.imagesEnabled) {
                    view?.evaluateJavascript("window.__db_toggle_images && window.__db_toggle_images(false);", null)
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                if (!url.startsWith("http")) return null
                val headers = request.requestHeaders ?: emptyMap()
                val blocking = settings.blockAds &&
                    siteConfigs.firstOrNull { it.host == hostOf(tab.url) }?.blockAds != false
                val blocked = blocking && BrowserEngine.isBlocked(url)
                val record = NetworkRecord(
                    id = ++recordId,
                    url = url,
                    method = request.method ?: "GET",
                    resourceType = BrowserEngine.resourceTypeOf(url, headers["Accept"]),
                    startedAt = System.currentTimeMillis(),
                    requestHeaders = headers,
                )
                if (blocked) {
                    record.blocked = true
                    record.status = 0
                    record.statusText = "blocked"
                    record.sizeBytes = 0
                }
                mainHandler.post {
                    tab.network.add(record)
                    if (tab.network.size > 400) tab.network.removeAt(0)
                    if (blocked) tab.blockedCount++
                }
                return if (blocked) {
                    WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
                } else {
                    null
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                val url = request?.url?.toString() ?: return
                val code = errorResponse?.statusCode ?: 0
                mainHandler.post {
                    tab.network.lastOrNull { it.url == url }?.let {
                        it.status = code
                        it.statusText = errorResponse?.reasonPhrase ?: "error"
                        it.mimeType = errorResponse?.mimeType ?: ""
                        it.responseHeaders = errorResponse?.responseHeaders ?: emptyMap()
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (request?.isForMainFrame == true) {
                    tab.loading = false
                    tab.crashed = true
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                tab.progress = newProgress / 100f
                tab.loading = newProgress < 100
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) tab.title = title
            }

            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                return true
            }
        }

        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            startDownload(url, userAgent, contentDisposition, mimeType, contentLength)
        }
    }

    private fun injectRuntime(view: WebView?) {
        view?.evaluateJavascript(BrowserEngine.devtoolsRuntime, null)
        if (settings.doNotTrack) {
            view?.evaluateJavascript(BrowserEngine.DO_NOT_TRACK_JS, null)
        }
    }

    private fun runUserScripts(view: WebView?, timing: String) {
        val url = view?.url ?: return
        scripts.filter { it.enabled && it.runAt == timing }.forEach { script ->
            val matches = script.matches.trim()
            val ok = matches == "*" || matches.isBlank() ||
                matches.split(',').any { pattern ->
                    val p = pattern.trim().removePrefix("*://").removeSuffix("/*").removeSuffix("*")
                    p.isNotBlank() && url.contains(p, ignoreCase = true)
                }
            if (ok) view.evaluateJavascript(BrowserEngine.userScriptWrapper(script.name, script.code), null)
        }
    }

    // ---------------- JS bridge ----------------
    inner class DevBridge(private val tab: TabState) {
        @JavascriptInterface
        fun consoleMessage(level: String, text: String) {
            mainHandler.post {
                tab.console.add(
                    ConsoleMessage(
                        id = ++recordId,
                        level = level,
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        source = hostOf(tab.url),
                    ),
                )
                if (tab.console.size > 500) tab.console.removeAt(0)
            }
        }

        @JavascriptInterface
        fun consoleClear() {
            mainHandler.post { tab.console.clear() }
        }

        @JavascriptInterface
        fun elementPicked(path: String) {
            mainHandler.post { pickedNodePath = path }
        }

        @JavascriptInterface
        fun fpsResult(fps: Int) {
            mainHandler.post { lastFps = fps }
        }

        @JavascriptInterface
        fun resourceTimings(payload: String) {
            mainHandler.post { mergeTimings(tab, payload) }
        }
    }

    private fun mergeTimings(tab: TabState, payload: String) {
        try {
            val array = json.parseToJsonElement(payload)
            val list = array as? kotlinx.serialization.json.JsonArray ?: return
            list.forEach { element ->
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@forEach
                val url = obj["url"]?.jsonPrimitive?.content ?: return@forEach
                val record = tab.network.lastOrNull { it.url == url }
                val dur = obj["dur"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val size = obj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val start = obj["start"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val cached = obj["cached"]?.jsonPrimitive?.content == "true"
                if (record != null) {
                    record.durationMs = dur
                    record.sizeBytes = size
                    record.startOffsetMs = start
                    record.fromCache = cached
                    if (record.status == 0 && !record.blocked) {
                        record.status = 200
                        record.statusText = if (cached) "200 (cache)" else "200"
                    }
                } else {
                    val type = obj["type"]?.jsonPrimitive?.content ?: "other"
                    val new = NetworkRecord(
                        id = ++recordId,
                        url = url,
                        method = "GET",
                        resourceType = when (type) {
                            "script" -> "js"
                            "link", "css" -> "css"
                            "img", "image" -> "img"
                            "xmlhttprequest", "fetch" -> "xhr"
                            else -> type
                        },
                        startedAt = System.currentTimeMillis(),
                    )
                    new.durationMs = dur
                    new.sizeBytes = size
                    new.startOffsetMs = start
                    new.fromCache = cached
                    new.status = 200
                    new.statusText = if (cached) "200 (cache)" else "200"
                    tab.network.add(new)
                }
            }
        } catch (e: Exception) {
            // malformed payload – ignore
        }
    }

    // ---------------- devtools helpers ----------------
    fun evalRaw(script: String, tab: TabState? = activeTab, callback: (String) -> Unit) {
        val wv = tab?.webView
        if (wv == null) {
            callback("null")
            return
        }
        wv.evaluateJavascript(script) { result -> callback(result ?: "null") }
    }

    fun evalString(script: String, tab: TabState? = activeTab, callback: (String) -> Unit) {
        evalRaw(script, tab) { raw -> callback(decodeJsString(raw)) }
    }

    fun decodeJsString(raw: String): String = try {
        if (raw.startsWith("\"")) json.decodeFromString<String>(raw) else raw
    } catch (e: Exception) {
        raw
    }

    fun runConsoleCommand(command: String, tab: TabState? = activeTab) {
        val t = tab ?: return
        t.console.add(
            ConsoleMessage(++recordId, "input", command, System.currentTimeMillis(), isInput = true),
        )
        evalString(BrowserEngine.wrapEval(command), t) { result ->
            val level = if (result.startsWith("Uncaught")) "error" else "result"
            t.console.add(ConsoleMessage(++recordId, level, result, System.currentTimeMillis()))
        }
    }

    fun setInspectMode(enabled: Boolean, tab: TabState? = activeTab) {
        evalRaw("window.__db_inspect_mode && window.__db_inspect_mode($enabled);", tab) {}
    }

    fun measureFps(tab: TabState? = activeTab) {
        lastFps = -1
        evalRaw("window.__db_fps && window.__db_fps();", tab) {}
    }

    fun clearConsole(tab: TabState? = activeTab) = tab?.console?.clear()
    fun clearNetwork(tab: TabState? = activeTab) {
        tab?.network?.clear()
        tab?.blockedCount = 0
    }

    fun cookiesFor(url: String): List<Pair<String, String>> {
        if (url.isBlank()) return emptyList()
        val raw = CookieManager.getInstance().getCookie(url) ?: return emptyList()
        return raw.split(";").mapNotNull {
            val idx = it.indexOf('=')
            if (idx <= 0) null else it.substring(0, idx).trim() to it.substring(idx + 1)
        }
    }

    // ---------------- bookmarks / history ----------------
    fun isBookmarked(url: String) = bookmarks.any { it.url == url }

    fun toggleBookmark(tab: TabState? = activeTab) {
        val t = tab ?: return
        if (t.url.isBlank()) return
        val existing = bookmarks.firstOrNull { it.url == t.url }
        if (existing != null) {
            bookmarks.remove(existing)
            toast("Bookmark removed")
        } else {
            bookmarks.add(0, BookmarkItem(UUID.randomUUID().toString(), t.displayTitle, t.url, "Bookmarks", System.currentTimeMillis()))
            toast("Bookmarked")
        }
        persistBookmarks()
    }

    fun addBookmark(title: String, url: String, folder: String = "Bookmarks") {
        bookmarks.add(0, BookmarkItem(UUID.randomUUID().toString(), title, url, folder, System.currentTimeMillis()))
        persistBookmarks()
    }

    fun removeBookmark(id: String) {
        bookmarks.removeAll { it.id == id }
        persistBookmarks()
    }

    private fun persistBookmarks() = viewModelScope.launch { repo.saveBookmarks(bookmarks.toList()) }

    private fun recordHistory(tab: TabState) {
        if (tab.incognito || !settings.saveHistory || tab.url.isBlank()) return
        val existing = history.firstOrNull { it.url == tab.url }
        if (existing != null) {
            history.remove(existing)
            history.add(0, existing.copy(title = tab.title, visitedAt = System.currentTimeMillis(), visits = existing.visits + 1))
        } else {
            history.add(0, HistoryItem(UUID.randomUUID().toString(), tab.title, tab.url, System.currentTimeMillis()))
        }
        while (history.size > 500) history.removeAt(history.size - 1)
        viewModelScope.launch { repo.saveHistory(history.toList()) }
    }

    fun removeHistory(id: String) {
        history.removeAll { it.id == id }
        viewModelScope.launch { repo.saveHistory(history.toList()) }
    }

    fun topSites(limit: Int = 8): List<HistoryItem> =
        history.groupBy { hostOf(it.url) }
            .map { entry -> entry.value.maxByOrNull { it.visits }!! }
            .sortedByDescending { it.visits }
            .take(limit)

    fun suggestions(query: String): List<Pair<String, String>> {
        if (!settings.searchSuggestions) return emptyList()
        if (query.isBlank()) {
            return history.take(6).map { it.title to it.url }
        }
        val q = query.lowercase()
        val fromHistory = history.filter {
            it.url.lowercase().contains(q) || it.title.lowercase().contains(q)
        }.take(5).map { it.title to it.url }
        val fromBookmarks = bookmarks.filter {
            it.url.lowercase().contains(q) || it.title.lowercase().contains(q)
        }.take(3).map { "\u2605 " + it.title to it.url }
        return (fromBookmarks + fromHistory).distinctBy { it.second }.take(7)
    }

    // ---------------- shortcuts ----------------
    fun addShortcut(title: String, url: String, icon: String? = null) {
        val fixed = if (url.contains("://")) url else "https://$url"
        shortcuts.add(Shortcut(UUID.randomUUID().toString(), title.ifBlank { hostOf(fixed) }, fixed, icon))
        persistShortcuts()
    }

    fun updateShortcut(shortcut: Shortcut) {
        val idx = shortcuts.indexOfFirst { it.id == shortcut.id }
        if (idx >= 0) shortcuts[idx] = shortcut
        persistShortcuts()
    }

    fun removeShortcut(id: String) {
        shortcuts.removeAll { it.id == id }
        persistShortcuts()
    }

    fun moveShortcut(from: Int, to: Int) {
        if (from !in shortcuts.indices || to !in shortcuts.indices) return
        val item = shortcuts.removeAt(from)
        shortcuts.add(to, item)
        persistShortcuts()
    }

    fun resetShortcuts() {
        shortcuts.clear()
        shortcuts.addAll(com.agon.app.data.DefaultShortcuts.list)
        persistShortcuts()
    }

    private fun persistShortcuts() = viewModelScope.launch { repo.saveShortcuts(shortcuts.toList()) }

    // ---------------- search engines ----------------
    fun setSearchEngine(id: String) = updateSettings { it.copy(searchEngineId = id) }

    fun addEngine(name: String, template: String) {
        val id = "custom_" + UUID.randomUUID().toString().take(6)
        val home = template.substringBefore("/search").substringBefore("/?").ifBlank { template }
        engines.add(SearchEngine(id, name, template, home, builtIn = false))
        viewModelScope.launch { repo.saveEngines(engines.toList()) }
    }

    fun removeEngine(id: String) {
        engines.removeAll { it.id == id && !it.builtIn }
        if (settings.searchEngineId == id) setSearchEngine("google")
        viewModelScope.launch { repo.saveEngines(engines.toList()) }
    }

    fun resetEngines() {
        engines.clear()
        engines.addAll(SearchEngines.defaults)
        viewModelScope.launch { repo.saveEngines(engines.toList()) }
    }

    // ---------------- scripts / extensions ----------------
    fun upsertScript(script: UserScript) {
        val idx = scripts.indexOfFirst { it.id == script.id }
        if (idx >= 0) scripts[idx] = script else scripts.add(script)
        viewModelScope.launch { repo.saveScripts(scripts.toList()) }
    }

    fun removeScript(id: String) {
        scripts.removeAll { it.id == id }
        viewModelScope.launch { repo.saveScripts(scripts.toList()) }
    }

    fun runScriptNow(script: UserScript) {
        evalRaw(BrowserEngine.userScriptWrapper(script.name, script.code)) {}
        toast("Executed \"${script.name}\"")
    }

    fun setExtensionEnabled(id: String, enabled: Boolean) {
        val idx = extensions.indexOfFirst { it.id == id }
        if (idx >= 0) extensions[idx] = extensions[idx].copy(enabled = enabled)
        viewModelScope.launch { repo.saveExtensions(extensions.toList()) }
    }

    fun installExtension(record: ExtensionRecord) {
        extensions.add(record)
        viewModelScope.launch { repo.saveExtensions(extensions.toList()) }
    }

    fun removeExtension(id: String) {
        extensions.removeAll { it.id == id }
        viewModelScope.launch { repo.saveExtensions(extensions.toList()) }
    }

    // ---------------- web apps ----------------
    fun addWebApp(item: WebAppItem) {
        webApps.add(item)
        viewModelScope.launch { repo.saveWebApps(webApps.toList()) }
    }

    fun removeWebApp(id: String) {
        webApps.removeAll { it.id == id }
        viewModelScope.launch { repo.saveWebApps(webApps.toList()) }
    }

    fun pinToHomeScreen(title: String, url: String) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val icon = withContext(Dispatchers.IO) { downloadIcon(faviconUrl(url, 192)) }
            val intent = Intent(context, Class.forName("com.agon.app.MainActivity")).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val builder = ShortcutInfoCompat.Builder(context, "webapp_" + hostOf(url))
                .setShortLabel(title.ifBlank { hostOf(url) })
                .setLongLabel(title.ifBlank { url })
                .setIntent(intent)
            if (icon != null) {
                builder.setIcon(IconCompat.createWithBitmap(icon))
            } else {
                builder.setIcon(IconCompat.createWithResource(context, com.agon.app.R.mipmap.ic_launcher))
            }
            val ok = try {
                ShortcutManagerCompat.requestPinShortcut(context, builder.build(), null)
            } catch (e: Exception) {
                false
            }
            toast(if (ok) "Shortcut added with site icon" else "Launcher refused the shortcut request")
        }
    }

    private fun downloadIcon(url: String): Bitmap? = try {
        val connection = java.net.URL(url).openConnection()
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.getInputStream().use { android.graphics.BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }

    // ---------------- site config ----------------
    fun siteConfigFor(url: String): SiteConfig {
        val host = hostOf(url)
        return siteConfigs.firstOrNull { it.host == host } ?: SiteConfig(host)
    }

    fun updateSiteConfig(config: SiteConfig) {
        val idx = siteConfigs.indexOfFirst { it.host == config.host }
        if (idx >= 0) siteConfigs[idx] = config else siteConfigs.add(config)
        viewModelScope.launch { repo.saveSiteConfigs(siteConfigs.toList()) }
        activeTab?.webView?.settings?.let { s ->
            s.javaScriptEnabled = config.javascript && settings.javascriptEnabled
            s.loadsImagesAutomatically = config.images && settings.imagesEnabled
            s.textZoom = config.zoom
        }
    }

    // ---------------- downloads ----------------
    fun startDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = -1L,
    ) {
        val context = getApplication<Application>()
        val fileName = try {
            URLUtil.guessFileName(url, contentDisposition, mimeType)
        } catch (e: Exception) {
            url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
        }
        val record = DownloadRecord(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            url = url,
            mimeType = mimeType ?: "",
            sizeBytes = contentLength,
            startedAt = System.currentTimeMillis(),
            status = "Downloading",
        )
        downloads.add(0, record)
        viewModelScope.launch { repo.saveDownloads(downloads.toList()) }
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription("DevBrowser download")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                if (userAgent != null) addRequestHeader("User-Agent", userAgent)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            toast("Downloading $fileName")
        } catch (e: Exception) {
            updateDownloadStatus(record.id, "Failed: ${e.message?.take(60)}")
            toast("Download failed")
        }
    }

    fun updateDownloadStatus(id: String, status: String) {
        val idx = downloads.indexOfFirst { it.id == id }
        if (idx >= 0) downloads[idx] = downloads[idx].copy(status = status)
        viewModelScope.launch { repo.saveDownloads(downloads.toList()) }
    }

    fun removeDownload(id: String) {
        downloads.removeAll { it.id == id }
        viewModelScope.launch { repo.saveDownloads(downloads.toList()) }
    }

    fun clearDownloads() {
        downloads.clear()
        viewModelScope.launch { repo.saveDownloads(emptyList()) }
    }

    // ---------------- saved pages ----------------
    fun savePageOffline(tab: TabState? = activeTab) {
        val t = tab ?: return
        val wv = t.webView ?: return
        val context = getApplication<Application>()
        val dir = java.io.File(context.filesDir, "saved")
        if (!dir.exists()) dir.mkdirs()
        val file = java.io.File(dir, "page_${System.currentTimeMillis()}.mht")
        wv.saveWebArchive(file.absolutePath, false) { result ->
            mainHandler.post {
                if (result != null) {
                    savedPages.add(
                        0,
                        SavedPage(
                            UUID.randomUUID().toString(), t.displayTitle, t.url,
                            System.currentTimeMillis(), java.io.File(result).length(), result,
                        ),
                    )
                    viewModelScope.launch { repo.saveSavedPages(savedPages.toList()) }
                    toast("Saved for offline reading")
                } else {
                    toast("Could not save this page")
                }
            }
        }
    }

    fun openSavedPage(page: SavedPage) {
        if (page.path.isBlank()) return
        newTab("file://" + page.path, false)
    }

    fun removeSavedPage(id: String) {
        savedPages.firstOrNull { it.id == id }?.let { page ->
            runCatching { java.io.File(page.path).delete() }
        }
        savedPages.removeAll { it.id == id }
        viewModelScope.launch { repo.saveSavedPages(savedPages.toList()) }
    }

    // ---------------- page actions ----------------
    fun share(url: String, title: String = "") {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, url)
        }
        val chooser = Intent.createChooser(intent, "Share link")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    fun openWith(url: String) {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val chooser = Intent.createChooser(intent, "Open with")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    fun translate(tab: TabState? = activeTab) {
        val t = tab ?: return
        if (t.url.isBlank()) return
        val target = "https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(t.url)
        navigate(target, t)
        toast("Translating via Google Translate")
    }

    fun toggleReaderMode(tab: TabState? = activeTab, dark: Boolean) {
        val t = tab ?: return
        if (t.readerMode) {
            t.readerMode = false
            t.webView?.reload()
            return
        }
        evalString("window.__db_readable ? window.__db_readable() : '';", t) { payload ->
            if (payload.isBlank()) {
                toast("Reader mode unavailable on this page")
                return@evalString
            }
            try {
                val obj = json.parseToJsonElement(payload) as kotlinx.serialization.json.JsonObject
                val title = obj["title"]?.jsonPrimitive?.content ?: t.title
                val html = obj["html"]?.jsonPrimitive?.content ?: ""
                if (html.isBlank()) {
                    toast("No readable article found")
                    return@evalString
                }
                val doc = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                    "<style>${BrowserEngine.readerCss(dark)}</style></head><body>" +
                    "<h1>$title</h1>$html</body></html>"
                t.webView?.loadDataWithBaseURL(t.url, doc, "text/html", "utf-8", null)
                t.readerMode = true
            } catch (e: Exception) {
                toast("Reader mode failed")
            }
        }
    }

    fun findInPage(query: String, tab: TabState? = activeTab) {
        val wv = tab?.webView ?: return
        wv.setFindListener { active, matches, _ ->
            findMatches = matches
            findIndex = if (matches == 0) 0 else active + 1
        }
        if (query.isBlank()) wv.clearMatches() else wv.findAllAsync(query)
    }

    fun findNext(forward: Boolean, tab: TabState? = activeTab) {
        tab?.webView?.findNext(forward)
    }

    fun readAloud(tab: TabState? = activeTab) {
        val t = tab ?: return
        if (speaking) {
            tts?.stop()
            speaking = false
            toast("Stopped reading")
            return
        }
        evalString("window.__db_text ? window.__db_text() : document.body.innerText;", t) { text ->
            if (text.isBlank()) {
                toast("Nothing to read on this page")
                return@evalString
            }
            val engine = tts
            if (engine == null) {
                tts = TextToSpeech(getApplication()) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        speakChunks(text)
                    } else {
                        toast("Text-to-speech unavailable")
                    }
                }
            } else {
                speakChunks(text)
            }
        }
    }

    private fun speakChunks(text: String) {
        val engine = tts ?: return
        speaking = true
        val chunks = text.chunked(3500)
        chunks.forEachIndexed { index, chunk ->
            engine.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "db_$index",
            )
        }
        toast("Reading page aloud")
    }

    // ---------------- privacy ----------------
    fun clearData(
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearDownloadsList: Boolean,
        clearStorage: Boolean,
        clearFormData: Boolean,
    ) {
        if (clearHistory) {
            history.clear()
            viewModelScope.launch { repo.saveHistory(emptyList()) }
        }
        if (clearCookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        if (clearCache) tabs.forEach { it.webView?.clearCache(true) }
        if (clearStorage) WebStorage.getInstance().deleteAllData()
        if (clearFormData) tabs.forEach { it.webView?.clearFormData() }
        if (clearDownloadsList) clearDownloads()
        toast("Selected data cleared")
    }

    fun toast(message: String) {
        toastMessage = message
    }

    fun consumeToast() {
        toastMessage = null
    }

    override fun onCleared() {
        tts?.shutdown()
        tabs.forEach { it.webView?.destroy() }
        super.onCleared()
    }
}
