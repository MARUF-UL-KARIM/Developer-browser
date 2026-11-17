package com.agon.app.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ConsoleMessage(
    val id: Long,
    val level: String,
    val text: String,
    val timestamp: Long,
    val source: String = "",
    val isInput: Boolean = false,
)

class NetworkRecord(
    val id: Long,
    val url: String,
    val method: String,
    val resourceType: String,
    val startedAt: Long,
    val requestHeaders: Map<String, String> = emptyMap(),
) {
    var status: Int by mutableStateOf(0)
    var statusText: String by mutableStateOf("pending")
    var mimeType: String by mutableStateOf("")
    var sizeBytes: Long by mutableStateOf(-1L)
    var durationMs: Int by mutableStateOf(-1)
    var startOffsetMs: Int by mutableStateOf(0)
    var blocked: Boolean by mutableStateOf(false)
    var responseHeaders: Map<String, String> by mutableStateOf(emptyMap())
    var fromCache: Boolean by mutableStateOf(false)

    val fileName: String
        get() = url.substringBefore('?').substringAfterLast('/').ifBlank { url.substringAfter("//").substringBefore('/') }
}

data class DomNode(
    val path: String,
    val tag: String,
    val id: String,
    val classes: String,
    val text: String,
    val childCount: Int,
    val attributes: Map<String, String>,
)

data class StorageEntry(val area: String, val key: String, val value: String)

data class PerfMetric(val label: String, val value: String, val group: String, val hint: String = "")

data class ResourceAsset(val url: String, val type: String, val sizeBytes: Long, val mime: String)
