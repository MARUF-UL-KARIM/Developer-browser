package com.agon.app.browser

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TabState(
    val id: String,
    val incognito: Boolean = false,
) {
    var url by mutableStateOf("")
    var title by mutableStateOf("New tab")
    var progress by mutableStateOf(0f)
    var loading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isHome by mutableStateOf(true)
    var desktopMode by mutableStateOf(false)
    var secure by mutableStateOf(false)
    var groupName by mutableStateOf("")
    var groupColor by mutableStateOf(0)
    var suspended by mutableStateOf(false)
    var blockedCount by mutableIntStateOf(0)
    var readerMode by mutableStateOf(false)
    var thumbnail by mutableStateOf<Bitmap?>(null)
    var lastVisited by mutableStateOf(System.currentTimeMillis())
    var crashed by mutableStateOf(false)
    var pendingRestoreUrl: String? = null

    val console = mutableStateListOf<ConsoleMessage>()
    val network = mutableStateListOf<NetworkRecord>()

    var webView: WebView? = null

    val displayTitle: String
        get() = when {
            isHome -> if (incognito) "New incognito tab" else "New tab"
            title.isNotBlank() -> title
            else -> url
        }
}
