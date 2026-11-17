package com.agon.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agon.app.data.ExtensionRecord
import com.agon.app.data.UserScript
import com.agon.app.viewmodel.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import java.util.zip.ZipInputStream

private data class ParsedExtension(
    val name: String,
    val version: String,
    val manifestVersion: Int,
    val description: String,
    val permissions: List<String>,
    val contentScripts: List<Pair<String, String>>,
    val hasServiceWorker: Boolean,
    val error: String? = null,
)

private val compatMatrix = listOf(
    Triple("Content scripts (document_start / idle)", "Supported", "Executed through the user-script runtime"),
    Triple("CSS injection", "Supported", "Injected as a style element"),
    Triple("declarativeNetRequest blocking rules", "Partial", "Host-level rules map to the native blocker"),
    Triple("chrome.storage.local", "Partial", "Backed by page localStorage per site"),
    Triple("Background service worker", "Unsupported", "WebView has no extension process"),
    Triple("Browser action popups / chrome.tabs", "Unsupported", "No extension UI surface in WebView"),
    Triple("Chrome Web Store one-click install", "Unsupported", "Store install API is restricted to Chrome"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(vm: BrowserViewModel, onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    var tabIndex by remember { mutableStateOf(0) }
    var installing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        installing = true
        scope.launch {
            val parsed = withContext(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext ParsedExtension("", "", 0, "", emptyList(), emptyList(), false, "File not readable")
                    parsePackage(bytes)
                } catch (e: Exception) {
                    ParsedExtension("", "", 0, "", emptyList(), emptyList(), false, e.message ?: "Unknown error")
                }
            }
            installing = false
            if (parsed.error != null) {
                lastResult = "Installation failed: ${parsed.error}"
                return@launch
            }
            val compatibility = when {
                parsed.contentScripts.isEmpty() && parsed.hasServiceWorker -> "Unsupported"
                parsed.hasServiceWorker -> "Partial"
                parsed.contentScripts.isNotEmpty() -> "Full"
                else -> "Partial"
            }
            vm.installExtension(
                ExtensionRecord(
                    id = UUID.randomUUID().toString(),
                    name = parsed.name,
                    version = parsed.version,
                    source = "Manifest V${parsed.manifestVersion} package",
                    permissions = parsed.permissions,
                    enabled = true,
                    compatibility = compatibility,
                    description = parsed.description,
                ),
            )
            parsed.contentScripts.forEachIndexed { index, (matches, code) ->
                vm.upsertScript(
                    UserScript(
                        id = UUID.randomUUID().toString(),
                        name = "${parsed.name} content script ${index + 1}",
                        matches = matches,
                        runAt = "document-end",
                        code = code,
                        enabled = true,
                    ),
                )
            }
            lastResult = "Installed ${parsed.name} \u00b7 ${parsed.contentScripts.size} content scripts registered" +
                if (parsed.hasServiceWorker) " \u00b7 background service worker cannot run in WebView" else ""
            tabIndex = 0
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Extensions") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                )
                TabRow(selectedTabIndex = tabIndex) {
                    listOf("Installed", "Install", "Compatibility").forEachIndexed { index, title ->
                        Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (installing) LinearProgressIndicator(Modifier.fillMaxWidth())
            lastResult?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            when (tabIndex) {
                0 -> LazyColumn(Modifier.weight(1f)) {
                    items(vm.extensions, key = { it.id }) { extension ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.size(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(extension.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "v${extension.version} \u00b7 ${extension.source}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = extension.enabled,
                                        onCheckedChange = { vm.setExtensionEnabled(extension.id, it) },
                                    )
                                }
                                if (extension.description.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(extension.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Compatibility: ${extension.compatibility}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (extension.compatibility) {
                                        "Full" -> MaterialTheme.colorScheme.secondary
                                        "Partial" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                )
                                if (extension.permissions.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Requested permissions", style = MaterialTheme.typography.labelLarge)
                                    extension.permissions.take(10).forEach {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Shield, null,
                                                Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Spacer(Modifier.size(6.dp))
                                            Text(it, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (extension.source != "Built-in") {
                                    TextButton(onClick = { vm.removeExtension(extension.id) }) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> Column(
                    Modifier
                        .weight(1f)
                        .padding(16.dp),
                ) {
                    Text("Install an extension", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "DevBrowser reads Manifest V2 and V3 packages (.crx / .zip), validates the manifest, " +
                            "shows requested permissions and registers the content scripts in the user-script " +
                            "runtime. Background service workers and browser-action popups cannot run inside " +
                            "Android WebView, so those parts are reported as unsupported instead of silently failing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = {
                        picker.launch(arrayOf("application/zip", "application/x-chrome-extension", "application/octet-stream", "*/*"))
                    }) { Text("Load .crx or .zip package") }
                    TextButton(onClick = {
                        onOpenUrl("https://chromewebstore.google.com/")
                    }) { Text("Open Chrome Web Store") }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Why the store's Add to Chrome button stays disabled: the store checks for Chrome's " +
                            "private installation API (chrome.webstorePrivate), which Google only exposes to " +
                            "Chrome itself. Spoofing the user agent does not provide it. Download the package " +
                            "instead and install it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(Modifier.weight(1f)) {
                    items(compatMatrix) { (feature, status, note) ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(feature, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    status,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (status) {
                                        "Supported" -> MaterialTheme.colorScheme.secondary
                                        "Partial" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                )
                            }
                            Text(
                                note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parsePackage(bytes: ByteArray): ParsedExtension {
    var offset = 0
    if (bytes.size > 4 && bytes[0] == 'C'.code.toByte() && bytes[1] == 'r'.code.toByte()) {
        var index = 0
        while (index < bytes.size - 4) {
            if (bytes[index] == 0x50.toByte() && bytes[index + 1] == 0x4B.toByte() &&
                bytes[index + 2] == 0x03.toByte() && bytes[index + 3] == 0x04.toByte()
            ) {
                offset = index
                break
            }
            index++
        }
        if (offset == 0) return ParsedExtension("", "", 0, "", emptyList(), emptyList(), false, "CRX archive is malformed")
    }

    val files = mutableMapOf<String, String>()
    ZipInputStream(bytes.inputStream(offset, bytes.size - offset)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.size < 2_000_000) {
                val name = entry.name
                if (name.endsWith(".json") || name.endsWith(".js") || name.endsWith(".css")) {
                    files[name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    val manifestRaw = files["manifest.json"]
        ?: files.entries.firstOrNull { it.key.endsWith("manifest.json") }?.value
        ?: return ParsedExtension("", "", 0, "", emptyList(), emptyList(), false, "manifest.json not found")

    return try {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val manifest = json.parseToJsonElement(manifestRaw).jsonObject
        val name = manifest["name"]?.jsonPrimitive?.content ?: "Unnamed extension"
        val version = manifest["version"]?.jsonPrimitive?.content ?: "0"
        val manifestVersion = manifest["manifest_version"]?.jsonPrimitive?.content?.toIntOrNull() ?: 2
        val description = manifest["description"]?.jsonPrimitive?.content ?: ""
        val permissions = mutableListOf<String>()
        (manifest["permissions"] as? JsonArray)?.forEach { permissions.add(it.jsonPrimitive.content) }
        (manifest["host_permissions"] as? JsonArray)?.forEach { permissions.add("host: " + it.jsonPrimitive.content) }
        val background = manifest["background"] as? JsonObject
        val hasServiceWorker = background?.get("service_worker") != null || background?.get("scripts") != null
        val contentScripts = mutableListOf<Pair<String, String>>()
        (manifest["content_scripts"] as? JsonArray)?.forEach { element ->
            val obj = element.jsonObject
            val matches = (obj["matches"] as? JsonArray)?.joinToString(",") {
                it.jsonPrimitive.content.removePrefix("*://").removeSuffix("/*").removeSuffix("*")
                    .ifBlank { "*" }
            } ?: "*"
            val code = StringBuilder()
            (obj["js"] as? JsonArray)?.forEach { fileName ->
                files[fileName.jsonPrimitive.content]?.let { code.append(it).append('\n') }
            }
            (obj["css"] as? JsonArray)?.forEach { fileName ->
                files[fileName.jsonPrimitive.content]?.let { css ->
                    code.append(
                        "(function(){var s=document.createElement('style');s.textContent=" +
                            JsonPrimitive(css).toString() + ";document.head.appendChild(s);})();\n",
                    )
                }
            }
            if (code.isNotBlank()) contentScripts.add(matches.ifBlank { "*" } to code.toString())
        }
        ParsedExtension(name, version, manifestVersion, description, permissions, contentScripts, hasServiceWorker)
    } catch (e: Exception) {
        ParsedExtension("", "", 0, "", emptyList(), emptyList(), false, "Invalid manifest: ${e.message}")
    }
}
