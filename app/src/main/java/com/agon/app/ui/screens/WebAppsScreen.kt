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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.agon.app.data.WebAppItem
import com.agon.app.data.hostOf
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.Favicon
import com.agon.app.viewmodel.BrowserViewModel
import java.util.UUID

private val presets = listOf(
    "YouTube" to "https://m.youtube.com",
    "Instagram" to "https://www.instagram.com",
    "Facebook" to "https://m.facebook.com",
    "X" to "https://x.com",
    "Reddit" to "https://www.reddit.com",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAppsScreen(vm: BrowserViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WebAppItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Apps Studio") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "Install web app") }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Text(
                "Install a website as a standalone app. The launcher shortcut uses the real site icon, " +
                    "and each app keeps its own user-agent and content-blocking profile. Cookie isolation " +
                    "is limited to what the WebView engine supports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.take(3).forEach { (name, url) ->
                    Chip(name, selected = false, onClick = {
                        vm.addWebApp(WebAppItem(UUID.randomUUID().toString(), name, url))
                        vm.toast("$name profile added")
                    })
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(vm.webApps, key = { it.id }) { app ->
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
                                Favicon(app.url, size = 42.dp, corner = 12.dp, customIcon = app.iconUrl)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        hostOf(app.url),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onOpen(app.url) }) { Icon(Icons.Default.OpenInNew, "Open") }
                                IconButton(onClick = { vm.removeWebApp(app.id) }) { Icon(Icons.Default.Delete, "Remove") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(
                                    if (app.userAgent == "desktop") "Desktop UA" else "Mobile UA",
                                    selected = app.userAgent == "desktop",
                                ) {
                                    vm.removeWebApp(app.id)
                                    vm.addWebApp(app.copy(userAgent = if (app.userAgent == "desktop") "mobile" else "desktop"))
                                }
                                Chip("Blocker ${if (app.blockAds) "on" else "off"}", selected = app.blockAds) {
                                    vm.removeWebApp(app.id)
                                    vm.addWebApp(app.copy(blockAds = !app.blockAds))
                                }
                                Chip("Isolated ${if (app.isolated) "on" else "off"}", selected = app.isolated) {
                                    vm.removeWebApp(app.id)
                                    vm.addWebApp(app.copy(isolated = !app.isolated))
                                }
                            }
                            TextButton(onClick = { vm.pinToHomeScreen(app.name, app.url) }) {
                                Icon(Icons.Default.AddToHomeScreen, null, Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add to home screen")
                            }
                        }
                    }
                }
                if (vm.webApps.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Web, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Text("No web apps yet", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        val target = editing
        var name by remember(target) { mutableStateOf(target?.name ?: (vm.activeTab?.title ?: "")) }
        var url by remember(target) { mutableStateOf(target?.url ?: (vm.activeTab?.url ?: "")) }
        var desktop by remember(target) { mutableStateOf(target?.userAgent == "desktop") }
        var blocker by remember(target) { mutableStateOf(target?.blockAds ?: true) }
        AlertDialog(
            onDismissRequest = { creating = false; editing = null },
            title = { Text("Install web app") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("App name") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Start URL") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Desktop user-agent", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = desktop, onCheckedChange = { desktop = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Content blocking", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = blocker, onCheckedChange = { blocker = it })
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = url.isNotBlank(),
                    onClick = {
                        val fixed = if (url.contains("://")) url else "https://$url"
                        vm.addWebApp(
                            WebAppItem(
                                UUID.randomUUID().toString(),
                                name.ifBlank { hostOf(fixed) },
                                fixed,
                                if (desktop) "desktop" else "mobile",
                                blocker,
                                true,
                            ),
                        )
                        vm.pinToHomeScreen(name.ifBlank { hostOf(fixed) }, fixed)
                        creating = false
                        editing = null
                    },
                ) { Text("Install") }
            },
            dismissButton = { TextButton(onClick = { creating = false; editing = null }) { Text("Cancel") } },
        )
    }
}
