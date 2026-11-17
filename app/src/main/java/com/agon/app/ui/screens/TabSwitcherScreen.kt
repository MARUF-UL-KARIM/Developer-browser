package com.agon.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agon.app.browser.TabState
import com.agon.app.data.hostOf
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.EmptyState
import com.agon.app.ui.components.Favicon
import com.agon.app.viewmodel.BrowserViewModel

private val groupColors = listOf(
    0xFF1B6EF3, 0xFF00A8B5, 0xFFE08A00, 0xFFD3455B, 0xFF8E5BD8, 0xFF2E9B5B,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherScreen(vm: BrowserViewModel, onBack: () -> Unit) {
    var incognitoView by remember { mutableStateOf(vm.incognitoMode) }
    var menuOpen by remember { mutableStateOf(false) }
    var groupTarget by remember { mutableStateOf<TabState?>(null) }

    val visible = vm.tabs.filter { it.incognito == incognitoView }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (incognitoView) "Incognito tabs" else "Tabs") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { incognitoView = !incognitoView }) {
                        Icon(
                            if (incognitoView) Icons.Default.Tab else Icons.Default.VisibilityOff,
                            "Switch session",
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Close all tabs") },
                                onClick = {
                                    menuOpen = false
                                    visible.forEach { vm.closeTab(it.id) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Reopen closed tab") },
                                leadingIcon = { Icon(Icons.Default.History, null) },
                                onClick = { menuOpen = false; vm.restoreLastClosed() },
                            )
                            DropdownMenuItem(
                                text = { Text("Suspend background tabs") },
                                onClick = {
                                    menuOpen = false
                                    vm.tabs.filter { it.id != vm.activeTabId }.forEach { vm.suspendTab(it) }
                                    vm.toast("Inactive tabs suspended to free memory")
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    vm.newTab(null, incognitoView)
                    onBack()
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(if (incognitoView) "New incognito tab" else "New tab") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (vm.recentlyClosed.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip(
                        "Reopen: ${vm.recentlyClosed.first().first.take(22)}",
                        selected = false,
                        onClick = { vm.restoreLastClosed(); onBack() },
                        icon = Icons.Default.History,
                    )
                }
            }
            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Tab,
                    title = "No open tabs",
                    subtitle = "Start a new tab to begin browsing.",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.id }) { tab ->
                        TabCard(
                            tab = tab,
                            active = tab.id == vm.activeTabId,
                            onSelect = { vm.selectTab(tab.id); onBack() },
                            onClose = { vm.closeTab(tab.id) },
                            onDuplicate = { vm.duplicateTab(tab.id) },
                            onGroup = { groupTarget = tab },
                        )
                    }
                }
            }
        }
    }

    val target = groupTarget
    if (target != null) {
        var name by remember(target) { mutableStateOf(target.groupName) }
        var color by remember(target) { mutableStateOf(target.groupColor) }
        AlertDialog(
            onDismissRequest = { groupTarget = null },
            title = { Text("Tab group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group name") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        groupColors.forEachIndexed { index, value ->
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(Color(value))
                                    .clickable { color = index },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (color == index) {
                                    Icon(Icons.Default.Label, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setTabGroup(target.id, name, color)
                    groupTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.setTabGroup(target.id, "", 0)
                    groupTarget = null
                }) { Text("Remove group") }
            },
        )
    }
}

@Composable
private fun TabCard(
    tab: TabState,
    active: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit,
    onGroup: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (tab.isHome) {
                    Icon(Icons.Default.Tab, null, Modifier.size(18.dp))
                } else {
                    Favicon(tab.url, size = 18.dp, corner = 4.dp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    tab.displayTitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.MoreVert, "Tab options", Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = { menu = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text("Group\u2026") },
                            leadingIcon = { Icon(Icons.Default.Label, null) },
                            onClick = { menu = false; onGroup() },
                        )
                        DropdownMenuItem(
                            text = { Text("Close") },
                            leadingIcon = { Icon(Icons.Default.Close, null) },
                            onClick = { menu = false; onClose() },
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, "Close tab", Modifier.size(15.dp))
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = tab.thumbnail
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!tab.isHome) Favicon(tab.url, size = 36.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (tab.isHome) "Home" else hostOf(tab.url),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (tab.suspended) {
                            Text(
                                "suspended",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }
            if (tab.groupName.isNotBlank()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(groupColors[tab.groupColor % groupColors.size])),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(tab.groupName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}
