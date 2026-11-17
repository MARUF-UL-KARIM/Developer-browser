package com.agon.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agon.app.browser.BrowserEngine
import com.agon.app.data.hostOf
import com.agon.app.ui.components.EmptyState
import com.agon.app.ui.components.Favicon
import com.agon.app.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: BrowserViewModel,
    initialTab: Int,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(initialTab.coerceIn(0, 3)) }
    var query by remember { mutableStateOf("") }
    val titles = listOf("Bookmarks", "History", "Downloads", "Saved")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Library") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                )
                TabRow(selectedTabIndex = selected) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = selected == index,
                            onClick = { selected = index },
                            text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                        )
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search ${titles[selected].lowercase()}") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            when (selected) {
                0 -> {
                    val items = vm.bookmarks.filter {
                        query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true)
                    }
                    if (items.isEmpty()) {
                        EmptyState(Icons.Default.Bookmarks, "No bookmarks", "Tap the bookmark icon in the menu to save a page.")
                    } else {
                        LazyColumn {
                            items(items, key = { it.id }) { item ->
                                LibraryRow(
                                    url = item.url,
                                    title = item.title,
                                    subtitle = "${item.folder} \u00b7 ${hostOf(item.url)}",
                                    onClick = { onOpen(item.url) },
                                    onShare = { vm.share(item.url, item.title) },
                                    onDelete = { vm.removeBookmark(item.id) },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val items = vm.history.filter {
                        query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true)
                    }
                    if (items.isEmpty()) {
                        EmptyState(Icons.Default.History, "No history yet", "Pages you visit appear here unless you browse in incognito.")
                    } else {
                        LazyColumn {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = {
                                        vm.clearData(true, false, false, false, false, false)
                                    }) { Text("Clear browsing history") }
                                }
                            }
                            items(items, key = { it.id }) { item ->
                                LibraryRow(
                                    url = item.url,
                                    title = item.title.ifBlank { hostOf(item.url) },
                                    subtitle = "${dateFormat.format(Date(item.visitedAt))} \u00b7 ${item.visits} visits",
                                    onClick = { onOpen(item.url) },
                                    onShare = { vm.share(item.url, item.title) },
                                    onDelete = { vm.removeHistory(item.id) },
                                )
                            }
                        }
                    }
                }
                2 -> {
                    val items = vm.downloads.filter { query.isBlank() || it.fileName.contains(query, true) }
                    if (items.isEmpty()) {
                        EmptyState(Icons.Default.Download, "No downloads", "Files you download are saved to the Downloads folder.")
                    } else {
                        LazyColumn {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = { vm.clearDownloads() }) { Text("Clear list") }
                                }
                            }
                            items(items, key = { it.id }) { item ->
                                LibraryRow(
                                    url = item.url,
                                    title = item.fileName,
                                    subtitle = "${item.status} \u00b7 ${BrowserEngine.prettySize(item.sizeBytes)} \u00b7 " +
                                        dateFormat.format(Date(item.startedAt)),
                                    onClick = { vm.openWith(item.url) },
                                    onShare = { vm.share(item.url, item.fileName) },
                                    onDelete = { vm.removeDownload(item.id) },
                                )
                            }
                        }
                    }
                }
                else -> {
                    val items = vm.savedPages.filter { query.isBlank() || it.title.contains(query, true) }
                    if (items.isEmpty()) {
                        EmptyState(Icons.Default.Cloud, "No saved pages", "Use Menu \u203a Save page offline to keep a page for later.")
                    } else {
                        LazyColumn {
                            items(items, key = { it.id }) { item ->
                                LibraryRow(
                                    url = item.url,
                                    title = item.title,
                                    subtitle = "${BrowserEngine.prettySize(item.sizeBytes)} \u00b7 " +
                                        dateFormat.format(Date(item.savedAt)) + " \u00b7 offline",
                                    onClick = { vm.openSavedPage(item); onBack() },
                                    onShare = { vm.share(item.url, item.title) },
                                    onDelete = { vm.removeSavedPage(item.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    url: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Favicon(url, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Share, "Share", Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, "Delete", Modifier.size(17.dp))
            }
        }
    }
}

@Composable
fun OpenInNewIcon() {
    Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
}
