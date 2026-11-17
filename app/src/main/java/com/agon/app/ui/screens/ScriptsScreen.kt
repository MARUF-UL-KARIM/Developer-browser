package com.agon.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.agon.app.data.UserScript
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.CodeBlock
import com.agon.app.ui.components.EmptyState
import com.agon.app.viewmodel.BrowserViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsScreen(vm: BrowserViewModel, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<UserScript?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Script manager") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = UserScript(UUID.randomUUID().toString(), "New script", "*", "document-end", "", false)
            }) { Icon(Icons.Default.Add, "New script") }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Text(
                "User scripts run in the page context with the permissions you grant. " +
                    "Scripts marked document-start run before page scripts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (vm.scripts.isEmpty()) {
                EmptyState(Icons.Default.Code, "No scripts", "Create a script to customise any website.")
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(vm.scripts, key = { it.id }) { script ->
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
                                    Column(Modifier.weight(1f)) {
                                        Text(script.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${script.matches} \u00b7 ${script.runAt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = script.enabled,
                                        onCheckedChange = { vm.upsertScript(script.copy(enabled = it)) },
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                CodeBlock(
                                    script.code.lines().take(3).joinToString("\n"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { editing = script }) { Text("Edit") }
                                    TextButton(onClick = { vm.runScriptNow(script) }) {
                                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                                        Text("Run now")
                                    }
                                    TextButton(onClick = { vm.removeScript(script.id) }) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val target = editing
    if (target != null) {
        var name by remember(target) { mutableStateOf(target.name) }
        var matches by remember(target) { mutableStateOf(target.matches) }
        var runAt by remember(target) { mutableStateOf(target.runAt) }
        var code by remember(target) { mutableStateOf(target.code) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Script") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = matches,
                        onValueChange = { matches = it },
                        label = { Text("Sites (comma separated, * for all)") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("document-start", runAt == "document-start") { runAt = "document-start" }
                        Chip("document-end", runAt == "document-end") { runAt = "document-end" }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("JavaScript") },
                        modifier = Modifier.heightIn(min = 120.dp, max = 220.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.upsertScript(target.copy(name = name, matches = matches, runAt = runAt, code = code))
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
