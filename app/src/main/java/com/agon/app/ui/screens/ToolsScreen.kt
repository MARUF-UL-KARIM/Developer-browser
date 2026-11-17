package com.agon.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agon.app.browser.BrowserEngine
import com.agon.app.data.UserScript
import com.agon.app.data.hostOf
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.CodeBlock
import com.agon.app.viewmodel.BrowserViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

private data class ToolEntry(val id: String, val name: String, val icon: ImageVector, val hint: String)

private val toolList = listOf(
    ToolEntry("viewsource", "View Source", Icons.Default.Code, "Document HTML"),
    ToolEntry("console", "JavaScript Console", Icons.Default.Terminal, "Evaluate in page"),
    ToolEntry("network", "Network Log", Icons.Default.NetworkCheck, "Live requests"),
    ToolEntry("resources", "Resource Inspector", Icons.Default.Image, "Assets on page"),
    ToolEntry("sniffer", "Resource Sniffer", Icons.Default.Download, "Downloadable media"),
    ToolEntry("json", "JSON Formatter", Icons.Default.DataObject, "Pretty print"),
    ToolEntry("html", "HTML Formatter", Icons.Default.Code, "Indent markup"),
    ToolEntry("css", "CSS Formatter", Icons.Default.Style, "Expand rules"),
    ToolEntry("js", "JavaScript Formatter", Icons.Default.Code, "Readable layout"),
    ToolEntry("useragent", "User-Agent Switcher", Icons.Default.Language, "Mobile / desktop"),
    ToolEntry("scripts", "Script Manager", Icons.Default.Code, "User scripts"),
    ToolEntry("cssedit", "CSS Editor", Icons.Default.Style, "Inject styles"),
    ToolEntry("inspector", "Page Inspector", Icons.Default.Build, "DOM tree"),
    ToolEntry("performance", "Website Performance", Icons.Default.Speed, "Timing metrics"),
    ToolEntry("cookies", "Cookie Viewer", Icons.Default.Cookie, "Site cookies"),
    ToolEntry("storage", "Storage Inspector", Icons.Default.Storage, "Local / session"),
    ToolEntry("info", "Website Information", Icons.Default.Info, "Meta and headers"),
    ToolEntry("qr", "QR Scanner", Icons.Default.QrCodeScanner, "Open scanned link"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    vm: BrowserViewModel,
    onBack: () -> Unit,
    onRoute: (String) -> Unit,
) {
    var tool by remember { mutableStateOf(vm.toolsTarget) }
    val tab = vm.activeTab
    val clipboard = LocalClipboardManager.current

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            vm.toast("Scan cancelled")
        } else {
            vm.navigate(contents)
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        vm.toolsTarget = null
        if (tool == "qr") {
            qrLauncher.launch(
                ScanOptions().setPrompt("Point the camera at a QR code").setBeepEnabled(false),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(toolList.firstOrNull { it.id == tool }?.name ?: "Developer tools") },
                navigationIcon = {
                    IconButton(onClick = { if (tool == null) onBack() else tool = null }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (tool) {
                null -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(toolList, key = { it.id }) { entry ->
                        Card(
                            onClick = {
                                when (entry.id) {
                                    "viewsource" -> { vm.devToolsPanel = 5; onRoute("devtools") }
                                    "console" -> { vm.devToolsPanel = 1; onRoute("devtools") }
                                    "network" -> { vm.devToolsPanel = 2; onRoute("devtools") }
                                    "inspector" -> { vm.devToolsPanel = 0; onRoute("devtools") }
                                    "performance" -> { vm.devToolsPanel = 4; onRoute("devtools") }
                                    "storage" -> { vm.devToolsPanel = 3; onRoute("devtools") }
                                    "scripts" -> onRoute("scripts")
                                    "qr" -> {
                                        tool = "qr"
                                        qrLauncher.launch(
                                            ScanOptions()
                                                .setPrompt("Point the camera at a QR code")
                                                .setBeepEnabled(false)
                                                .setOrientationLocked(false),
                                        )
                                    }
                                    else -> tool = entry.id
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            ),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Icon(entry.icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(10.dp))
                                Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text(
                                    entry.hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                "json" -> FormatterTool("JSON", "{\"paste\":\"json here\"}") { BrowserEngine.formatJson(it) }
                "html" -> FormatterTool("HTML", "<div><p>markup</p></div>") { BrowserEngine.formatMarkup(it) }
                "css" -> FormatterTool("CSS", "body{color:red;margin:0}") { BrowserEngine.formatCss(it) }
                "js" -> FormatterTool("JavaScript", "function a(){return 1;}") { BrowserEngine.formatCss(it) }
                "useragent" -> UserAgentTool(vm)
                "cssedit" -> CssEditorTool(vm)
                "cookies" -> CookieTool(vm)
                "info" -> SiteInfoTool(vm)
                "resources" -> ResourceTool(vm, downloadable = false)
                "sniffer" -> ResourceTool(vm, downloadable = true)
                "qr" -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Camera scanner", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The camera opens in a dedicated scanner. Scanned links open in a new tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = {
                        qrLauncher.launch(
                            ScanOptions().setPrompt("Point the camera at a QR code").setBeepEnabled(false),
                        )
                    }) { Text("Scan again") }
                }
                else -> Text("Tool unavailable", Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun FormatterTool(label: String, placeholder: String, format: (String) -> String) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("$label input") },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 220.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { output = format(input) }) { Text("Format") }
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(output))
            }) { Text("Copy result") }
            TextButton(onClick = { input = ""; output = "" }) { Text("Clear") }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {
            CodeBlock(output.ifBlank { "Formatted output appears here" }, Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun UserAgentTool(vm: BrowserViewModel) {
    var custom by remember { mutableStateOf(vm.settings.customUserAgent) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Active mode: ${vm.settings.userAgentMode}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Android mobile", vm.settings.userAgentMode == "mobile") {
                vm.updateSettings { it.copy(userAgentMode = "mobile") }
                vm.reload()
            }
            Chip("Desktop Chrome", vm.settings.userAgentMode == "desktop") {
                vm.updateSettings { it.copy(userAgentMode = "desktop") }
                vm.reload()
            }
            Chip("Custom", vm.settings.userAgentMode == "custom") {
                vm.updateSettings { it.copy(userAgentMode = "custom") }
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = custom,
            onValueChange = { custom = it },
            label = { Text("Custom user-agent string") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                vm.updateSettings { it.copy(customUserAgent = custom, userAgentMode = "custom") }
                vm.reload()
                vm.toast("Custom user-agent applied")
            }) { Text("Apply") }
            TextButton(onClick = {
                custom = ""
                vm.updateSettings { it.copy(customUserAgent = "", userAgentMode = "mobile") }
                vm.reload()
            }) { Text("Reset to default") }
            TextButton(onClick = {
                custom = BrowserEngine.DESKTOP_UA
            }) { Text("Fill desktop UA") }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Per-site desktop mode is available from Site configuration; it overrides this global setting " +
                "for a single host only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CssEditorTool(vm: BrowserViewModel) {
    var css by remember { mutableStateOf("body { filter: contrast(1.05); }") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Inject CSS into ${hostOf(vm.activeTab?.url ?: "")}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = css,
            onValueChange = { css = it },
            label = { Text("CSS") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 320.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                val script = "(function(){var s=document.getElementById('__db_css')||document.createElement('style');" +
                    "s.id='__db_css';s.textContent=${BrowserEngine.jsStringLiteral(css)};" +
                    "document.head.appendChild(s);})();"
                vm.evalRaw(script) {}
                vm.toast("CSS applied to the live page")
            }) { Text("Apply to page") }
            TextButton(onClick = {
                vm.upsertScript(
                    UserScript(
                        id = UUID.randomUUID().toString(),
                        name = "CSS for ${hostOf(vm.activeTab?.url ?: "site")}",
                        matches = hostOf(vm.activeTab?.url ?: ""),
                        runAt = "document-end",
                        code = "var s=document.createElement('style');s.textContent=" +
                            BrowserEngine.jsStringLiteral(css) + ";document.head.appendChild(s);",
                        enabled = true,
                    ),
                )
                vm.toast("Saved as persistent user script")
            }) { Text("Save for this site") }
            TextButton(onClick = {
                vm.evalRaw("(function(){var s=document.getElementById('__db_css'); if(s) s.remove();})();") {}
            }) { Text("Remove") }
        }
    }
}

@Composable
private fun CookieTool(vm: BrowserViewModel) {
    val url = vm.activeTab?.url ?: ""
    var cookies by remember { mutableStateOf(vm.cookiesFor(url)) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${cookies.size} cookies for ${hostOf(url)}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { cookies = vm.cookiesFor(url) }) { Text("Refresh") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(cookies) { (key, value) ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    CodeBlock(key, color = MaterialTheme.colorScheme.primary)
                    CodeBlock(value, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (cookies.isEmpty()) {
                item {
                    Text(
                        "No cookies stored for this site.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        TextButton(
            onClick = {
                vm.clearData(false, true, false, false, false, false)
                cookies = emptyList()
            },
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(Icons.Default.Delete, null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Delete all cookies")
        }
    }
}

@Composable
private fun SiteInfoTool(vm: BrowserViewModel) {
    var meta by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        vm.evalString("window.__db_meta ? window.__db_meta() : '{}';") { payload ->
            val out = mutableListOf<Pair<String, String>>()
            try {
                val obj = Json.parseToJsonElement(payload).jsonObject
                listOf("title", "url", "charset", "lang", "referrer", "viewport", "dpr", "ua").forEach { key ->
                    obj[key]?.jsonPrimitive?.content?.let { out.add(key to it) }
                }
                obj["metas"]?.jsonArray?.forEach { entry ->
                    val pair = entry.jsonArray
                    out.add(
                        (pair.getOrNull(0)?.jsonPrimitive?.content ?: "") to
                            (pair.getOrNull(1)?.jsonPrimitive?.content ?: ""),
                    )
                }
            } catch (e: Exception) {
                out.add("error" to "Could not read page metadata")
            }
            meta = out
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(meta) { (key, value) ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(key, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(value, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (meta.isEmpty()) {
            item { Text("Reading page metadata\u2026", Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun ResourceTool(vm: BrowserViewModel, downloadable: Boolean) {
    var assets by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var filter by remember { mutableStateOf("all") }
    LaunchedEffect(Unit) {
        vm.evalString("window.__db_assets ? window.__db_assets() : '[]';") { payload ->
            assets = try {
                Json.parseToJsonElement(payload).jsonArray.map {
                    val obj = it.jsonObject
                    (obj["url"]?.jsonPrimitive?.content ?: "") to (obj["type"]?.jsonPrimitive?.content ?: "")
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    val shown = assets.filter {
        (filter == "all" || it.second == filter) &&
            (!downloadable || it.second == "image" || it.second == "media" || it.second == "file")
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("all", "image", "media", "file", "script", "stylesheet").forEach { type ->
                if (!downloadable || type in listOf("all", "image", "media", "file")) {
                    Chip(type, filter == type) { filter = type }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(shown) { (url, type) ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            url.substringAfterLast('/').substringBefore('?').ifBlank { url },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "$type \u00b7 ${hostOf(url)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { vm.startDownload(url) }) {
                        Icon(Icons.Default.Download, "Download")
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("No resources detected", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Open a page first. DRM-protected and access-restricted streams are intentionally not listed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchIcon() {
    Icon(Icons.Default.Search, null, Modifier.size(16.dp))
}
