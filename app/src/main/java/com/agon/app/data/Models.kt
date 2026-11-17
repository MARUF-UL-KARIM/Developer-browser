package com.agon.app.data

import kotlinx.serialization.Serializable

@Serializable
data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrl: String,
    val homeUrl: String,
    val builtIn: Boolean = true,
)

object SearchEngines {
    val defaults: List<SearchEngine> = listOf(
        SearchEngine("google", "Google", "https://www.google.com/search?q=%s", "https://www.google.com"),
        SearchEngine("bing", "Microsoft Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com"),
        SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com"),
        SearchEngine("yahoo", "Yahoo!", "https://search.yahoo.com/search?p=%s", "https://www.yahoo.com"),
        SearchEngine("yandex", "Yandex", "https://yandex.com/search/?text=%s", "https://yandex.com"),
        SearchEngine("baidu", "Baidu", "https://www.baidu.com/s?wd=%s", "https://www.baidu.com"),
        SearchEngine("sogou", "Sogou", "https://www.sogou.com/web?query=%s", "https://www.sogou.com"),
        SearchEngine("naver", "Naver", "https://search.naver.com/search.naver?query=%s", "https://www.naver.com"),
        SearchEngine("so360", "360 Search", "https://www.so.com/s?q=%s", "https://www.so.com"),
        SearchEngine("qwant", "Qwant", "https://www.qwant.com/?q=%s", "https://www.qwant.com"),
        SearchEngine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s", "https://www.ecosia.org"),
        SearchEngine("perplexity", "Perplexity", "https://www.perplexity.ai/search?q=%s", "https://www.perplexity.ai"),
        SearchEngine("youtube", "YouTube", "https://www.youtube.com/results?search_query=%s", "https://www.youtube.com"),
        SearchEngine("reddit", "Reddit", "https://www.reddit.com/search/?q=%s", "https://www.reddit.com"),
        SearchEngine("x", "X", "https://x.com/search?q=%s", "https://x.com"),
        SearchEngine("mdn", "MDN Web Docs", "https://developer.mozilla.org/en-US/search?q=%s", "https://developer.mozilla.org"),
    )
}

@Serializable
data class Shortcut(
    val id: String,
    val title: String,
    val url: String,
    val customIcon: String? = null,
    val hidden: Boolean = false,
)

object DefaultShortcuts {
    val list: List<Shortcut> = listOf(
        Shortcut("s_google", "Google", "https://www.google.com"),
        Shortcut("s_youtube", "YouTube", "https://www.youtube.com"),
        Shortcut("s_github", "GitHub", "https://github.com"),
        Shortcut("s_reddit", "Reddit", "https://www.reddit.com"),
        Shortcut("s_x", "X", "https://x.com"),
        Shortcut("s_mdn", "MDN", "https://developer.mozilla.org"),
        Shortcut("s_so", "Stack Overflow", "https://stackoverflow.com"),
        Shortcut("s_wikipedia", "Wikipedia", "https://www.wikipedia.org"),
    )
}

@Serializable
data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String,
    val folder: String = "Bookmarks",
    val createdAt: Long = 0L,
)

@Serializable
data class HistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val visitedAt: Long = 0L,
    val visits: Int = 1,
)

@Serializable
data class DownloadRecord(
    val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String = "",
    val sizeBytes: Long = -1L,
    val startedAt: Long = 0L,
    val status: String = "Queued",
)

@Serializable
data class SavedPage(
    val id: String,
    val title: String,
    val url: String,
    val savedAt: Long,
    val sizeBytes: Long = 0L,
    val path: String = "",
)

@Serializable
data class UserScript(
    val id: String,
    val name: String,
    val matches: String = "*",
    val runAt: String = "document-end",
    val code: String = "",
    val enabled: Boolean = true,
)

@Serializable
data class ExtensionRecord(
    val id: String,
    val name: String,
    val version: String,
    val source: String,
    val permissions: List<String> = emptyList(),
    val enabled: Boolean = true,
    val compatibility: String = "Partial",
    val description: String = "",
)

@Serializable
data class WebAppItem(
    val id: String,
    val name: String,
    val url: String,
    val userAgent: String = "mobile",
    val blockAds: Boolean = true,
    val isolated: Boolean = true,
    val iconUrl: String? = null,
)

@Serializable
data class SiteConfig(
    val host: String,
    val javascript: Boolean = true,
    val images: Boolean = true,
    val desktop: Boolean = false,
    val blockAds: Boolean = true,
    val zoom: Int = 100,
)

@Serializable
data class BrowserSettings(
    val searchEngineId: String = "google",
    val themeMode: String = "system",
    val javascriptEnabled: Boolean = true,
    val imagesEnabled: Boolean = true,
    val blockAds: Boolean = true,
    val saveHistory: Boolean = true,
    val doNotTrack: Boolean = true,
    val desktopByDefault: Boolean = false,
    val userAgentMode: String = "mobile",
    val customUserAgent: String = "",
    val textZoom: Int = 100,
    val showWallpaper: Boolean = true,
    val wallpaperId: String = "tech",
    val showShortcuts: Boolean = true,
    val showRecent: Boolean = true,
    val searchSuggestions: Boolean = true,
    val devToolsDark: Boolean = true,
    val remoteDebugging: Boolean = false,
    val menuOrder: List<String> = emptyList(),
    val hiddenMenuItems: List<String> = emptyList(),
)

data class Wallpaper(val id: String, val name: String, val category: String, val colors: List<Long>)

object Wallpapers {
    val list = listOf(
        Wallpaper("tech", "Terminal", "Technology", listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364)),
        Wallpaper("nature", "Forest", "Nature", listOf(0xFF134E5E, 0xFF71B280)),
        Wallpaper("mountain", "Alpine", "Mountains", listOf(0xFF283048, 0xFF859398)),
        Wallpaper("abstract", "Nebula", "Abstract", listOf(0xFF42275A, 0xFF734B6D, 0xFFBC4E9C)),
        Wallpaper("minimal", "Paper", "Minimal", listOf(0xFFEEF2F7, 0xFFD9E2EC)),
        Wallpaper("sunset", "Ember", "Abstract", listOf(0xFFFF512F, 0xFFDD2476)),
        Wallpaper("ocean", "Deep Blue", "Nature", listOf(0xFF2193B0, 0xFF6DD5ED)),
        Wallpaper("none", "None", "Minimal", listOf(0xFF000000, 0xFF000000)),
    )

    fun byId(id: String): Wallpaper = list.firstOrNull { it.id == id } ?: list.first()
}

fun hostOf(url: String): String = try {
    val u = java.net.URI(url)
    (u.host ?: url).removePrefix("www.")
} catch (e: Exception) {
    url.removePrefix("https://").removePrefix("http://").substringBefore('/').removePrefix("www.")
}

fun faviconUrl(url: String, size: Int = 128): String {
    val host = hostOf(url)
    return "https://www.google.com/s2/favicons?sz=$size&domain=$host"
}
