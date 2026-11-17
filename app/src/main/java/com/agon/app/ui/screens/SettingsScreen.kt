package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agon.app.data.Wallpapers
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.SectionHeader
import com.agon.app.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: BrowserViewModel, onBack: () -> Unit) {
    var engineDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp),
        ) {
            item { SectionHeader("Search") }
            item {
                SettingRow(
                    title = "Default search engine",
                    subtitle = vm.searchEngine.name,
                    onClick = { engineDialog = true },
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.engines) { engine ->
                        Chip(engine.name, engine.id == vm.settings.searchEngineId) { vm.setSearchEngine(engine.id) }
                    }
                }
            }
            item {
                SwitchRow("Search suggestions", "Suggest from history and bookmarks", vm.settings.searchSuggestions) {
                    vm.updateSettings { s -> s.copy(searchSuggestions = it) }
                }
            }

            item { SectionHeader("Homepage") }
            item {
                SwitchRow("Show shortcut grid", "Site icons below the search field", vm.settings.showShortcuts) {
                    vm.updateSettings { s -> s.copy(showShortcuts = it) }
                }
            }
            item {
                SwitchRow("Show recently visited", "Quick access to recent pages", vm.settings.showRecent) {
                    vm.updateSettings { s -> s.copy(showRecent = it) }
                }
            }
            item {
                SwitchRow("Wallpaper", "Use a background on the start page", vm.settings.showWallpaper) {
                    vm.updateSettings { s -> s.copy(showWallpaper = it) }
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(Wallpapers.list) { wallpaper ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(60.dp, 86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(wallpaper.colors.map { Color(it) }))
                                    .clickable { vm.updateSettings { s -> s.copy(wallpaperId = wallpaper.id) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (vm.settings.wallpaperId == wallpaper.id) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                            }
                            Text(wallpaper.name, style = MaterialTheme.typography.labelSmall)
                            Text(
                                wallpaper.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Content") }
            item {
                SwitchRow("JavaScript", "Required by most modern websites", vm.settings.javascriptEnabled) {
                    vm.updateSettings { s -> s.copy(javascriptEnabled = it) }
                    vm.reload()
                }
            }
            item {
                SwitchRow("Load images", "Disable to save data", vm.settings.imagesEnabled) {
                    vm.updateSettings { s -> s.copy(imagesEnabled = it) }
                    vm.reload()
                }
            }
            item {
                SwitchRow("Content blocker", "Blocks known ad and tracker hosts", vm.settings.blockAds) {
                    vm.updateSettings { s -> s.copy(blockAds = it) }
                }
            }
            item {
                SwitchRow("Desktop site by default", "Request desktop pages on every site", vm.settings.desktopByDefault) {
                    vm.updateSettings { s -> s.copy(desktopByDefault = it) }
                    vm.reload()
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Text size: ${vm.settings.textZoom}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = vm.settings.textZoom.toFloat(),
                        onValueChange = { vm.updateSettings { s -> s.copy(textZoom = it.toInt()) } },
                        valueRange = 50f..220f,
                    )
                }
            }

            item { SectionHeader("Privacy and security") }
            item {
                SwitchRow("Save browsing history", "Incognito tabs are never recorded", vm.settings.saveHistory) {
                    vm.updateSettings { s -> s.copy(saveHistory = it) }
                }
            }
            item {
                SwitchRow("Send Do Not Track", "Adds the DNT request header", vm.settings.doNotTrack) {
                    vm.updateSettings { s -> s.copy(doNotTrack = it) }
                }
            }
            item {
                SwitchRow(
                    "Remote debugging (chrome://inspect)",
                    "Off by default. Enables USB inspection of this app's WebViews.",
                    vm.settings.remoteDebugging,
                ) {
                    vm.updateSettings { s -> s.copy(remoteDebugging = it) }
                    vm.toast(if (it) "Remote debugging enabled for this session" else "Remote debugging disabled")
                }
            }
            item {
                SettingRow("Clear browsing data", "History, cookies, cache, site storage") { clearDialog = true }
            }

            item { SectionHeader("Developer") }
            item {
                SettingRow("User agent", vm.settings.userAgentMode) {
                    vm.toolsTarget = "useragent"
                    vm.toast("Open Tools \u203a User-Agent Switcher")
                }
            }
            item {
                SwitchRow("Dark DevTools theme", "Monospace panels use the dark palette", vm.settings.devToolsDark) {
                    vm.updateSettings { s -> s.copy(devToolsDark = it) }
                }
            }

            item { SectionHeader("Appearance") }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (id, label) ->
                        Chip(label, vm.settings.themeMode == id) {
                            vm.updateSettings { s -> s.copy(themeMode = id) }
                        }
                    }
                }
            }

            item { SectionHeader("About") }
            item {
                SettingRow("Browser engine", "Android System WebView (Chromium)") { aboutDialog = true }
            }
            item {
                SettingRow("DevBrowser", "Version 2.0 \u00b7 Material 3 \u00b7 Kotlin + Compose") { aboutDialog = true }
            }
        }
    }

    if (engineDialog) {
        EngineDialog(vm) { engineDialog = false }
    }
    if (clearDialog) {
        var history by remember { mutableStateOf(true) }
        var cookies by remember { mutableStateOf(true) }
        var cache by remember { mutableStateOf(true) }
        var storage by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Clear browsing data") },
            text = {
                Column {
                    CheckRow("Browsing history", history) { history = it }
                    CheckRow("Cookies and site data", cookies) { cookies = it }
                    CheckRow("Cache", cache) { cache = it }
                    CheckRow("Website storage", storage) { storage = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearData(history, cookies, cache, false, storage, false)
                    clearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancel") } },
        )
    }
    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            title = { Text("Engine and capabilities") },
            text = {
                Column {
                    Text(
                        "DevBrowser renders with the Android System WebView, which is Chromium. " +
                            "It receives security updates through Google Play, keeps the APK small and gives " +
                            "full HTML5/CSS/JS compatibility.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "Chromium rendering" to "Supported",
                        "Resource Timing / Navigation Timing" to "Supported",
                        "Request interception and blocking" to "Supported",
                        "Live DOM, CSS and storage inspection" to "Supported",
                        "Response body capture" to "Not exposed by WebView",
                        "Manifest V3 service workers" to "Not supported in WebView",
                        "Chrome Web Store direct install" to "Restricted to Chrome",
                        "HTTP/3" to "Depends on the installed WebView build",
                    ).forEach { (feature, status) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(feature, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(
                                status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status == "Supported") {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { aboutDialog = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun EngineDialog(vm: BrowserViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search engines") },
        text = {
            Column {
                vm.engines.forEach { engine ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.setSearchEngine(engine.id) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(engine.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (engine.id == vm.settings.searchEngineId) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        if (!engine.builtIn) {
                            IconButton(onClick = { vm.removeEngine(engine.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Custom engine name") }, singleLine = true)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = template,
                    onValueChange = { template = it },
                    label = { Text("URL template with %s") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && template.contains("%s"),
                onClick = {
                    vm.addEngine(name, template)
                    name = ""
                    template = ""
                },
            ) { Text("Add engine") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { vm.resetEngines() }) { Text("Restore") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
