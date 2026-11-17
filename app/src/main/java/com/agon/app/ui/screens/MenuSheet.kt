package com.agon.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agon.app.browser.TabState
import com.agon.app.data.SiteConfig
import com.agon.app.data.hostOf
import com.agon.app.viewmodel.BrowserViewModel

data class MenuAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val subtitle: String = "",
    val action: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuSheet(
    vm: BrowserViewModel,
    tab: TabState?,
    onDismiss: () -> Unit,
    onRoute: (String) -> Unit,
    onFind: () -> Unit,
    onSiteInfo: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customizeOpen by remember { mutableStateOf(false) }
    var clearOpen by remember { mutableStateOf(false) }
    var textSizeOpen by remember { mutableStateOf(false) }
    var orientationOpen by remember { mutableStateOf(false) }

    fun close(block: () -> Unit) {
        block()
        onDismiss()
    }

    val bookmarked = tab != null && vm.isBookmarked(tab.url)
    val actions = listOf(
        MenuAction("bookmarks", "Bookmarks", Icons.Default.Bookmarks) { close { onRoute("library/0") } },
        MenuAction("history", "History", Icons.Default.History) { close { onRoute("library/1") } },
        MenuAction("downloads", "Downloads", Icons.Default.Download) { close { onRoute("library/2") } },
        MenuAction("saved", "Saved pages", Icons.Default.Cloud) { close { onRoute("library/3") } },
        MenuAction("incognito", "Incognito mode", Icons.Default.VisibilityOff, if (vm.incognitoMode) "On" else "Off") {
            close { vm.setIncognito(!vm.incognitoMode) }
        },
        MenuAction("devtools", "DevTools", Icons.Default.Build, "Elements, console, network") {
            close { vm.devToolsPanel = 0; onRoute("devtools") }
        },
        MenuAction("tools", "Developer tools suite", Icons.Default.Terminal, "18 utilities") { close { onRoute("tools") } },
        MenuAction("find", "Find in page", Icons.Default.Search) { close { onFind() } },
        MenuAction("desktop", "Desktop site", Icons.Default.DesktopWindows, if (tab?.desktopMode == true) "On" else "Off") {
            close { vm.toggleDesktopMode(tab) }
        },
        MenuAction("share", "Share", Icons.Default.Share) { close { vm.share(tab?.url ?: "", tab?.title ?: "") } },
        MenuAction("save", "Save page offline", Icons.Default.Save) { close { vm.savePageOffline(tab) } },
        MenuAction("translate", "Translate", Icons.Default.Translate) { close { vm.translate(tab) } },
        MenuAction(
            "reader", "Reader mode", Icons.Default.ChromeReaderMode,
            if (tab?.readerMode == true) "On" else "",
        ) { close { vm.toggleReaderMode(tab, dark = vm.settings.devToolsDark) } },
        MenuAction("viewsource", "View source", Icons.Default.Code) {
            close { vm.devToolsPanel = 5; onRoute("devtools") }
        },
        MenuAction("network", "Network log", Icons.Default.NetworkCheck, "${tab?.network?.size ?: 0} requests") {
            close { vm.devToolsPanel = 2; onRoute("devtools") }
        },
        MenuAction("sniffer", "Resource sniffer", Icons.Default.Download) {
            close { vm.toolsTarget = "sniffer"; onRoute("tools") }
        },
        MenuAction("useragent", "User agent", Icons.Default.Language, vm.settings.userAgentMode) {
            close { vm.toolsTarget = "useragent"; onRoute("tools") }
        },
        MenuAction("qr", "Scan QR code", Icons.Default.QrCodeScanner) {
            close { vm.toolsTarget = "qr"; onRoute("tools") }
        },
        MenuAction("fullscreen", "Full screen", Icons.Default.Fullscreen) { close { vm.fullScreen = true } },
        MenuAction(
            "images", "Show images", Icons.Default.Image,
            if (vm.settings.imagesEnabled) "Enabled" else "Disabled",
        ) {
            close {
                vm.updateSettings { it.copy(imagesEnabled = !it.imagesEnabled) }
                vm.reload(tab)
            }
        },
        MenuAction("readaloud", "Read aloud", Icons.Default.RecordVoiceOver, if (vm.speaking) "Playing" else "") {
            close { vm.readAloud(tab) }
        },
        MenuAction("orientation", "Orientation", Icons.Default.ScreenRotation) { orientationOpen = true },
        MenuAction("siteconfig", "Site configuration", Icons.Default.Tune, hostOf(tab?.url ?: "")) {
            close { onSiteInfo() }
        },
        MenuAction("scripts", "Scripts", Icons.Default.Code, "${vm.scripts.count { it.enabled }} active") {
            close { onRoute("scripts") }
        },
        MenuAction("extensions", "Extensions", Icons.Default.Extension, "${vm.extensions.count { it.enabled }} enabled") {
            close { onRoute("extensions") }
        },
        MenuAction("webapps", "Web Apps Studio", Icons.Default.Web, "${vm.webApps.size} installed") {
            close { onRoute("webapps") }
        },
        MenuAction("addhome", "Add to home screen", Icons.Default.AddToHomeScreen) {
            close { vm.pinToHomeScreen(tab?.displayTitle ?: "", tab?.url ?: "") }
        },
        MenuAction("textsize", "Text size", Icons.Default.TextFields, "${vm.settings.textZoom}%") { textSizeOpen = true },
        MenuAction("cleardata", "Clear data", Icons.Default.DeleteSweep) { clearOpen = true },
        MenuAction("openwith", "Open with", Icons.Default.OpenInNew) { close { vm.openWith(tab?.url ?: "") } },
        MenuAction("customize", "Customize menu", Icons.Default.Apps) { customizeOpen = true },
        MenuAction("settings", "Settings", Icons.Default.Settings) { close { onRoute("settings") } },
    )

    val hidden = vm.settings.hiddenMenuItems.toSet()
    val order = vm.settings.menuOrder
    val ordered = if (order.isEmpty()) {
        actions
    } else {
        actions.sortedBy { action ->
            val index = order.indexOf(action.id)
            if (index < 0) Int.MAX_VALUE else index
        }
    }.filter { it.id !in hidden }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.heightIn(max = 620.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickIcon(Icons.Default.Add, "New tab") { close { vm.newTab(null, vm.incognitoMode) } }
                QuickIcon(Icons.Default.VisibilityOff, "Incognito") { close { vm.newTab(null, true); vm.setIncognito(true) } }
                QuickIcon(
                    if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    if (bookmarked) "Saved" else "Bookmark",
                ) { close { vm.toggleBookmark(tab) } }
                QuickIcon(Icons.Default.Share, "Share") { close { vm.share(tab?.url ?: "", tab?.title ?: "") } }
                QuickIcon(Icons.Default.Build, "DevTools") { close { onRoute("devtools") } }
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.fillMaxWidth()) {
                items(ordered, key = { it.id }) { action ->
                    Surface(onClick = action.action, color = MaterialTheme.colorScheme.surface) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(action.icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(18.dp))
                            Text(action.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (action.subtitle.isNotBlank()) {
                                Text(
                                    action.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (customizeOpen) {
        CustomizeMenuDialog(vm, actions) { customizeOpen = false }
    }
    if (clearOpen) {
        ClearDataDialog(vm) { clearOpen = false; onDismiss() }
    }
    if (textSizeOpen) {
        TextSizeDialog(vm) { textSizeOpen = false }
    }
    if (orientationOpen) {
        AlertDialog(
            onDismissRequest = { orientationOpen = false },
            title = { Text("Screen orientation") },
            text = {
                Column {
                    listOf("Automatic" to 0, "Portrait" to 1, "Landscape" to 2).forEach { (label, mode) ->
                        TextButton(onClick = {
                            lockOrientation(context, mode)
                            orientationOpen = false
                            onDismiss()
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { orientationOpen = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun QuickIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, label, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CustomizeMenuDialog(
    vm: BrowserViewModel,
    actions: List<MenuAction>,
    onDismiss: () -> Unit,
) {
    var order by remember {
        mutableStateOf(
            if (vm.settings.menuOrder.isEmpty()) actions.map { it.id }
            else vm.settings.menuOrder + actions.map { it.id }.filter { it !in vm.settings.menuOrder },
        )
    }
    var hidden by remember { mutableStateOf(vm.settings.hiddenMenuItems.toSet()) }
    val labels = actions.associate { it.id to it.label }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize menu") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                order.forEachIndexed { index, id ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(labels[id] ?: id, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val list = order.toMutableList()
                                    val item = list.removeAt(index)
                                    list.add(index - 1, item)
                                    order = list
                                }
                            },
                            enabled = index > 0,
                        ) { Icon(Icons.Default.KeyboardArrowUp, "Up", Modifier.size(18.dp)) }
                        IconButton(
                            onClick = {
                                if (index < order.size - 1) {
                                    val list = order.toMutableList()
                                    val item = list.removeAt(index)
                                    list.add(index + 1, item)
                                    order = list
                                }
                            },
                            enabled = index < order.size - 1,
                        ) { Icon(Icons.Default.KeyboardArrowDown, "Down", Modifier.size(18.dp)) }
                        Switch(
                            checked = id !in hidden,
                            onCheckedChange = { visible ->
                                hidden = if (visible) hidden - id else hidden + id
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.updateSettings { it.copy(menuOrder = order, hiddenMenuItems = hidden.toList()) }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                vm.updateSettings { it.copy(menuOrder = emptyList(), hiddenMenuItems = emptyList()) }
                onDismiss()
            }) { Text("Restore default") }
        },
    )
}

@Composable
private fun ClearDataDialog(vm: BrowserViewModel, onDismiss: () -> Unit) {
    var history by remember { mutableStateOf(true) }
    var cookies by remember { mutableStateOf(true) }
    var cache by remember { mutableStateOf(true) }
    var downloads by remember { mutableStateOf(false) }
    var storage by remember { mutableStateOf(false) }
    var formData by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear browsing data") },
        text = {
            Column {
                CheckRow("Browsing history", history) { history = it }
                CheckRow("Cookies and site data", cookies) { cookies = it }
                CheckRow("Cached files", cache) { cache = it }
                CheckRow("Website storage (localStorage, IndexedDB)", storage) { storage = it }
                CheckRow("Saved form data", formData) { formData = it }
                CheckRow("Download history", downloads) { downloads = it }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Passwords are never removed by this action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.clearData(history, cookies, cache, downloads, storage, formData)
                onDismiss()
            }) { Text("Clear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TextSizeDialog(vm: BrowserViewModel, onDismiss: () -> Unit) {
    var zoom by remember { mutableStateOf(vm.settings.textZoom.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Text size") },
        text = {
            Column {
                Text("${zoom.toInt()}%", style = MaterialTheme.typography.titleMedium)
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 50f..220f, steps = 16)
                Text(
                    "Applies to all pages. Page zoom (pinch) stays independent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.updateSettings { it.copy(textZoom = zoom.toInt()) }
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteInfoSheet(vm: BrowserViewModel, tab: TabState?, onDismiss: () -> Unit) {
    val url = tab?.url ?: ""
    val host = hostOf(url)
    var config by remember { mutableStateOf(vm.siteConfigFor(url)) }
    val cookies = remember(url) { vm.cookiesFor(url) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(host.ifBlank { "New tab" }, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                if (tab?.secure == true) "Connection is secure (HTTPS)" else "Connection is not secure",
                style = MaterialTheme.typography.bodyMedium,
                color = if (tab?.secure == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${cookies.size} cookies \u00b7 ${tab?.blockedCount ?: 0} trackers blocked \u00b7 " +
                    "${tab?.network?.size ?: 0} requests",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text("Permissions for this site", style = MaterialTheme.typography.titleMedium)
            SiteToggle("JavaScript", config.javascript) { config = config.copy(javascript = it); vm.updateSiteConfig(config) }
            SiteToggle("Images", config.images) { config = config.copy(images = it); vm.updateSiteConfig(config) }
            SiteToggle("Desktop site", config.desktop) { config = config.copy(desktop = it); vm.updateSiteConfig(config) }
            SiteToggle("Content blocking", config.blockAds) { config = config.copy(blockAds = it); vm.updateSiteConfig(config) }
            Spacer(Modifier.height(10.dp))
            Text("Zoom: ${config.zoom}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = config.zoom.toFloat(),
                onValueChange = { config = config.copy(zoom = it.toInt()) },
                onValueChangeFinished = { vm.updateSiteConfig(config) },
                valueRange = 50f..200f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    vm.clearData(false, true, true, false, true, false)
                    onDismiss()
                }) { Text("Clear site data") }
                TextButton(onClick = { vm.reload(tab); onDismiss() }) { Text("Reload") }
            }
        }
    }
}

@Composable
private fun SiteToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun MenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp),
    )
}

fun defaultSiteConfig(host: String) = SiteConfig(host)
