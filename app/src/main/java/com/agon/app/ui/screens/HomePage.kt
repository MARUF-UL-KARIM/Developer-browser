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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.data.Shortcut
import com.agon.app.data.Wallpapers
import com.agon.app.data.hostOf
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.Favicon
import com.agon.app.viewmodel.BrowserViewModel

@Composable
fun HomePage(
    vm: BrowserViewModel,
    onOmnibox: () -> Unit,
    onOpenEngines: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val wallpaper = Wallpapers.byId(vm.settings.wallpaperId)
    val useWallpaper = vm.settings.showWallpaper && wallpaper.id != "none"
    val brush = if (useWallpaper) {
        Brush.verticalGradient(wallpaper.colors.map { Color(it) })
    } else {
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background),
        )
    }
    val onWall = if (useWallpaper && wallpaper.id != "minimal") Color.White else MaterialTheme.colorScheme.onBackground
    var addDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Shortcut?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp, top = 24.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Terminal, null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "DevBrowser",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = onWall,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (vm.incognitoMode) "Incognito \u00b7 nothing is saved" else "Browse. Inspect. Ship.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onWall.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            item {
                Surface(
                    onClick = onOmnibox,
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(52.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Search, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Search ${vm.searchEngine.name} or type a URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            Icons.Default.Mic, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.engines.take(8)) { engine ->
                        Chip(
                            label = engine.name,
                            selected = engine.id == vm.settings.searchEngineId,
                            onClick = { vm.setSearchEngine(engine.id) },
                        )
                    }
                    item {
                        Chip(label = "All engines", selected = false, onClick = onOpenEngines, icon = Icons.Default.Apps)
                    }
                }
                Spacer(Modifier.height(22.dp))
            }

            if (vm.settings.showShortcuts) {
                item {
                    val visible = vm.shortcuts.filter { !it.hidden }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (useWallpaper) 0.92f else 1f),
                        ),
                    ) {
                        Column(Modifier.padding(vertical = 14.dp)) {
                            val rows = visible.chunked(4)
                            rows.forEach { row ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    row.forEach { shortcut ->
                                        ShortcutTile(
                                            shortcut = shortcut,
                                            onClick = { onNavigate(shortcut.url) },
                                            onEdit = { editing = shortcut },
                                            onRemove = { vm.removeShortcut(shortcut.id) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, start = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(onClick = { addDialog = true }) {
                                    Icon(Icons.Default.Add, null, Modifier.size(17.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add shortcut")
                                }
                                TextButton(onClick = { vm.resetShortcuts() }) { Text("Restore defaults") }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (vm.settings.showRecent && !vm.incognitoMode && vm.history.isNotEmpty()) {
                item {
                    Text(
                        "Recently visited",
                        style = MaterialTheme.typography.titleMedium,
                        color = onWall,
                        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                    )
                }
                items(vm.history.take(6)) { entry ->
                    Surface(
                        onClick = { onNavigate(entry.url) },
                        color = MaterialTheme.colorScheme.surface.copy(alpha = if (useWallpaper) 0.9f else 1f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 3.dp),
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Favicon(entry.url, size = 30.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.title.ifBlank { hostOf(entry.url) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    hostOf(entry.url),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickAction("DevTools", Icons.Default.Build, Modifier.weight(1f)) { onOpenRoute("devtools") }
                    QuickAction("Bookmarks", Icons.Default.Bookmarks, Modifier.weight(1f)) { onOpenRoute("library") }
                    QuickAction("History", Icons.Default.History, Modifier.weight(1f)) { onOpenRoute("library") }
                }
            }
        }
    }

    if (addDialog || editing != null) {
        val target = editing
        var title by remember(target) { mutableStateOf(target?.title ?: "") }
        var url by remember(target) { mutableStateOf(target?.url ?: "") }
        AlertDialog(
            onDismissRequest = { addDialog = false; editing = null },
            title = { Text(if (target == null) "Add shortcut" else "Edit shortcut") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "The real site icon is resolved automatically from the website favicon.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = url.isNotBlank(),
                    onClick = {
                        if (target == null) {
                            vm.addShortcut(title, url)
                        } else {
                            vm.updateShortcut(target.copy(title = title, url = url))
                        }
                        addDialog = false
                        editing = null
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { addDialog = false; editing = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Favicon(shortcut.url, size = 46.dp, corner = 23.dp, customIcon = shortcut.customIcon)
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text("Remove") }, onClick = { menu = false; onRemove() })
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            shortcut.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "\u22ee",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { menu = true }
                .padding(horizontal = 10.dp),
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
