package com.agon.app.ui.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.browser.BrowserEngine
import com.agon.app.browser.DomNode
import com.agon.app.browser.NetworkRecord
import com.agon.app.browser.PerfMetric
import com.agon.app.browser.StorageEntry
import com.agon.app.browser.TabState
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.CodeBlock
import com.agon.app.ui.theme.CodeAttr
import com.agon.app.ui.theme.CodeTag
import com.agon.app.ui.theme.CodeValue
import com.agon.app.ui.theme.LogError
import com.agon.app.ui.theme.LogInfo
import com.agon.app.ui.theme.LogSuccess
import com.agon.app.ui.theme.LogWarn
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

private val devJson = Json { ignoreUnknownKeys = true; isLenient = true }

private val panels = listOf("Elements", "Console", "Network", "Storage", "Performance", "Sources")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsScreen(vm: BrowserViewModel, onBack: () -> Unit) {
    val tab = vm.activeTab
    var selected by remember { mutableStateOf(vm.devToolsPanel.coerceIn(0, panels.size - 1)) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("DevTools", style = MaterialTheme.typography.titleMedium)
                            Text(
                                tab?.url?.ifBlank { "about:home" } ?: "No page",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Close DevTools") }
                    },
                    actions = {
                        IconButton(onClick = { vm.reload(tab) }) { Icon(Icons.Default.Refresh, "Reload page") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                ScrollableTabRow(
                    selectedTabIndex = selected,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    panels.forEachIndexed { index, label ->
                        Tab(
                            selected = selected == index,
                            onClick = { selected = index; vm.devToolsPanel = index },
                            text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (tab == null || tab.isHome) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Speed, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Open a web page to inspect it", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "DevTools attaches to the active tab. Elements, Console, Network, Storage, " +
                            "Performance and Sources all read live data from the Chromium WebView engine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                when (selected) {
                    0 -> ElementsPanel(vm, tab)
                    1 -> ConsolePanel(vm, tab)
                    2 -> NetworkPanel(vm, tab)
                    3 -> StoragePanel(vm, tab)
                    4 -> PerformancePanel(vm, tab)
                    else -> SourcesPanel(vm, tab)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Elements */

private fun parseNodes(payload: String): List<DomNode> = try {
    val array = devJson.parseToJsonElement(payload) as? JsonArray ?: JsonArray(emptyList())
    array.map { element ->
        val obj = element.jsonObject
        DomNode(
            path = obj["path"]?.jsonPrimitive?.content ?: "",
            tag = obj["tag"]?.jsonPrimitive?.content ?: "div",
            id = obj["id"]?.jsonPrimitive?.content ?: "",
            classes = obj["cls"]?.jsonPrimitive?.content ?: "",
            text = obj["text"]?.jsonPrimitive?.content ?: "",
            childCount = obj["kids"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            attributes = (obj["attrs"] as? JsonObject)?.entries?.associate { (k, v) ->
                k to (v as? JsonPrimitive)?.content.orEmpty()
            } ?: emptyMap(),
        )
    }
} catch (e: Exception) {
    emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElementsPanel(vm: BrowserViewModel, tab: TabState) {
    val children = remember(tab.id) { mutableStateMapOf<String, List<DomNode>>() }
    val expanded = remember(tab.id) { mutableStateMapOf<String, Boolean>() }
    var root by remember(tab.id) { mutableStateOf<List<DomNode>>(emptyList()) }
    var selectedNode by remember(tab.id) { mutableStateOf<DomNode?>(null) }
    var styles by remember { mutableStateOf<JsonObject?>(null) }
    var query by remember { mutableStateOf("") }
    var inspectMode by remember { mutableStateOf(false) }
    var editAttr by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun loadChildren(path: String) {
        vm.evalString("window.__db_children ? window.__db_children(${BrowserEngine.jsStringLiteral(path)}) : '[]';", tab) {
            children[path] = parseNodes(it)
        }
    }

    fun select(node: DomNode) {
        selectedNode = node
        styles = null
        vm.evalString("window.__db_styles ? window.__db_styles(${BrowserEngine.jsStringLiteral(node.path)}) : '{}';", tab) {
            styles = try {
                devJson.parseToJsonElement(it) as? JsonObject
            } catch (e: Exception) {
                null
            }
        }
        vm.evalRaw("window.__db_highlight && window.__db_highlight(${BrowserEngine.jsStringLiteral(node.path)});", tab) {}
    }

    LaunchedEffect(tab.id) {
        vm.evalString("window.__db_root ? window.__db_root() : '[]';", tab) { payload ->
            root = parseNodes(payload)
            expanded[""] = true
            loadChildren("")
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { vm.pickedNodePath }.collect { path ->
            if (path != null) {
                inspectMode = false
                vm.evalString("window.__db_children ? window.__db_children(${BrowserEngine.jsStringLiteral(path)}) : '[]';", tab) {}
                vm.evalString(
                    "(function(){var el=window.__db_resolve(${BrowserEngine.jsStringLiteral(path)});" +
                        "if(!el) return '[]'; var a={}; for(var i=0;i<el.attributes.length;i++)a[el.attributes[i].name]=el.attributes[i].value;" +
                        "return JSON.stringify([{path:${BrowserEngine.jsStringLiteral(path)},tag:el.tagName.toLowerCase(),id:el.id||''," +
                        "cls:(typeof el.className==='string'?el.className:''),text:(el.children.length===0?(el.textContent||'').trim().substring(0,120):'')," +
                        "kids:el.children.length,attrs:a}]);})();",
                    tab,
                ) { payload ->
                    parseNodes(payload).firstOrNull()?.let { select(it) }
                }
                vm.pickedNodePath = null
            }
        }
    }

    val flat = remember(root, children.toMap(), expanded.toMap()) {
        val out = mutableListOf<Pair<DomNode, Int>>()
        fun walk(node: DomNode, depth: Int) {
            out.add(node to depth)
            if (expanded[node.path] == true) {
                children[node.path]?.forEach { walk(it, depth + 1) }
            }
        }
        root.forEach { walk(it, 0) }
        out
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("CSS selector, e.g. .header a", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    vm.evalString("window.__db_search ? window.__db_search(${BrowserEngine.jsStringLiteral(query)}) : '[]';", tab) {
                        val results = parseNodes(it)
                        root = results.ifEmpty { root }
                        results.firstOrNull()?.let { node -> select(node) }
                    }
                }),
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(17.dp)) },
            )
            IconButton(onClick = {
                inspectMode = !inspectMode
                vm.setInspectMode(inspectMode, tab)
                vm.toast(if (inspectMode) "Tap an element on the page" else "Inspect mode off")
            }) {
                Icon(
                    Icons.Default.HighlightAlt, "Inspect element",
                    tint = if (inspectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                children.clear()
                expanded.clear()
                vm.evalString("window.__db_root ? window.__db_root() : '[]';", tab) { payload ->
                    root = parseNodes(payload)
                    expanded[""] = true
                    loadChildren("")
                }
            }) { Icon(Icons.Default.Refresh, "Refresh DOM") }
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            itemsIndexed(flat) { _, (node, depth) ->
                DomRow(
                    node = node,
                    depth = depth,
                    expanded = expanded[node.path] == true,
                    selected = selectedNode?.path == node.path,
                    onToggle = {
                        val now = expanded[node.path] != true
                        expanded[node.path] = now
                        if (now && children[node.path] == null) loadChildren(node.path)
                    },
                    onSelect = { select(node) },
                )
            }
        }

        val node = selectedNode
        if (node != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
            ) {
                Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CodeBlock(
                            "<${node.tag}${if (node.id.isNotBlank()) " #${node.id}" else ""}>",
                            color = CodeTag,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            vm.evalRaw("window.__db_delete && window.__db_delete(${BrowserEngine.jsStringLiteral(node.path)});", tab) {}
                            selectedNode = null
                            vm.toast("Element removed (live change, resets on reload)")
                        }) { Icon(Icons.Default.Delete, "Delete node", Modifier.size(18.dp)) }
                        IconButton(onClick = { editAttr = "" to "" }) {
                            Icon(Icons.Default.Edit, "Edit attribute", Modifier.size(18.dp))
                        }
                    }
                    Text(
                        "Live DOM edits apply instantly and disappear on reload.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Attributes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    node.attributes.forEach { (key, value) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { editAttr = key to value }
                                .padding(vertical = 2.dp),
                        ) {
                            CodeBlock("$key=", color = CodeAttr)
                            CodeBlock("\"$value\"", color = CodeValue, maxLines = 2, modifier = Modifier.weight(1f))
                        }
                    }
                    if (node.attributes.isEmpty()) {
                        CodeBlock("(no attributes)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val styleObj = styles
                    if (styleObj != null) {
                        Spacer(Modifier.height(10.dp))
                        val box = styleObj["box"]?.jsonObject
                        if (box != null) {
                            Text("Box model", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            BoxModel(box)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Computed styles", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        styleObj["computed"]?.jsonObject?.forEach { (prop, value) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.evalRaw(
                                            "window.__db_setstyle && window.__db_setstyle(" +
                                                "${BrowserEngine.jsStringLiteral(node.path)}," +
                                                "${BrowserEngine.jsStringLiteral(prop)}," +
                                                "${BrowserEngine.jsStringLiteral("")});",
                                            tab,
                                        ) {}
                                        vm.toast("Disabled $prop on this element")
                                    },
                            ) {
                                CodeBlock("$prop: ", color = CodeAttr)
                                CodeBlock(value.jsonPrimitive.content, color = CodeValue, maxLines = 1, modifier = Modifier.weight(1f))
                            }
                        }
                        val matched = styleObj["matched"]?.jsonArray
                        if (matched != null && matched.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Matched CSS rules", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            matched.take(12).forEach {
                                CodeBlock(it.jsonPrimitive.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                }
            }
        }
    }

    val attrEdit = editAttr
    if (attrEdit != null && selectedNode != null) {
        var name by remember(attrEdit) { mutableStateOf(attrEdit.first) }
        var value by remember(attrEdit) { mutableStateOf(attrEdit.second) }
        AlertDialog(
            onDismissRequest = { editAttr = null },
            title = { Text("Edit attribute") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val path = selectedNode!!.path
                    vm.evalRaw(
                        "window.__db_setattr && window.__db_setattr(${BrowserEngine.jsStringLiteral(path)}," +
                            "${BrowserEngine.jsStringLiteral(name)},${BrowserEngine.jsStringLiteral(value)});",
                        tab,
                    ) {}
                    val updated = selectedNode!!.attributes.toMutableMap()
                    if (value.isBlank()) updated.remove(name) else updated[name] = value
                    selectedNode = selectedNode!!.copy(attributes = updated)
                    editAttr = null
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { editAttr = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BoxModel(box: JsonObject) {
    fun v(key: String) = box[key]?.jsonPrimitive?.content ?: "0"
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
    ) {
        CodeBlock("margin   ${v("marginTop")} ${v("marginRight")} ${v("marginBottom")} ${v("marginLeft")}", color = CodeAttr)
        CodeBlock("border   ${v("borderTop")} ${v("borderRight")} ${v("borderBottom")} ${v("borderLeft")}", color = CodeAttr)
        CodeBlock("padding  ${v("paddingTop")} ${v("paddingRight")} ${v("paddingBottom")} ${v("paddingLeft")}", color = CodeAttr)
        CodeBlock("content  ${v("width")} \u00d7 ${v("height")} px  @ (${v("left")}, ${v("top")})", color = CodeValue)
    }
}

@Composable
private fun DomRow(
    node: DomNode,
    depth: Int,
    expanded: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
            )
            .clickable { onSelect() }
            .padding(start = (8 + depth * 12).dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.childCount > 0) {
            Icon(
                if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                "Expand",
                Modifier
                    .size(16.dp)
                    .clickable { onToggle() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Spacer(Modifier.width(4.dp))
        Row(Modifier.weight(1f)) {
            CodeBlock("<${node.tag}", color = CodeTag)
            if (node.id.isNotBlank()) CodeBlock(" id=\"${node.id}\"", color = CodeAttr, maxLines = 1)
            if (node.classes.isNotBlank()) {
                CodeBlock(
                    " class=\"${node.classes.take(28)}\"",
                    color = CodeValue,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            CodeBlock(">", color = CodeTag)
            if (node.text.isNotBlank()) {
                CodeBlock(
                    " ${node.text.take(40)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (node.childCount > 0) {
            Text(
                "${node.childCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ------------------------------------------------------------------ Console */

@Composable
private fun ConsolePanel(vm: BrowserViewModel, tab: TabState) {
    var filter by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val filtered = tab.console.filter { message ->
        val levelOk = when (filter) {
            "errors" -> message.level == "error"
            "warnings" -> message.level == "warn"
            "info" -> message.level == "info" || message.level == "log"
            "debug" -> message.level == "debug"
            else -> true
        }
        levelOk && (search.isBlank() || message.text.contains(search, ignoreCase = true))
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Filter output", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
            )
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(filtered.joinToString("\n") { "[${it.level}] ${it.text}" }))
                vm.toast("Console output copied")
            }) { Icon(Icons.Default.ContentCopy, "Copy output") }
            IconButton(onClick = { vm.clearConsole(tab) }) { Icon(Icons.Default.Block, "Clear console") }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val counts = tab.console.groupingBy { it.level }.eachCount()
            items(
                listOf(
                    "all" to "All ${tab.console.size}",
                    "errors" to "Errors ${counts["error"] ?: 0}",
                    "warnings" to "Warnings ${counts["warn"] ?: 0}",
                    "info" to "Info ${(counts["info"] ?: 0) + (counts["log"] ?: 0)}",
                    "debug" to "Debug ${counts["debug"] ?: 0}",
                ),
            ) { (id, label) ->
                Chip(label = label, selected = filter == id, onClick = { filter = id })
            }
        }
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(filtered) { message ->
                val color = when (message.level) {
                    "error" -> LogError
                    "warn" -> LogWarn
                    "info" -> LogInfo
                    "result" -> LogSuccess
                    "input" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            when (message.level) {
                                "error" -> LogError.copy(alpha = 0.08f)
                                "warn" -> LogWarn.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            },
                        )
                        .clickable {
                            clipboard.setText(AnnotatedString(message.text))
                            vm.toast("Copied to clipboard")
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    CodeBlock(
                        when (message.level) {
                            "input" -> "\u203a "
                            "result" -> "\u2039 "
                            else -> "\u00b7 "
                        },
                        color = color,
                    )
                    CodeBlock(message.text, color = color, modifier = Modifier.weight(1f))
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No console output yet", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "console.log / warn / error / table / time are all captured from the page context.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    placeholder = { Text("Evaluate JavaScript in page context", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (command.isNotBlank()) {
                            vm.runConsoleCommand(command, tab)
                            command = ""
                        }
                    }),
                )
                IconButton(onClick = {
                    if (command.isNotBlank()) {
                        vm.runConsoleCommand(command, tab)
                        command = ""
                    }
                }) { Icon(Icons.Default.PlayArrow, "Run", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

/* ------------------------------------------------------------------ Network */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkPanel(vm: BrowserViewModel, tab: TabState) {
    var filter by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<NetworkRecord?>(null) }
    val clipboard = LocalClipboardManager.current

    val records = tab.network.filter { record ->
        val typeOk = when (filter) {
            "all" -> true
            "xhr" -> record.resourceType == "xhr"
            "js" -> record.resourceType == "js"
            "css" -> record.resourceType == "css"
            "img" -> record.resourceType == "img"
            "doc" -> record.resourceType == "doc"
            "blocked" -> record.blocked
            "failed" -> record.status >= 400
            else -> true
        }
        typeOk && (search.isBlank() || record.url.contains(search, ignoreCase = true))
    }
    val totalBytes = tab.network.sumOf { if (it.sizeBytes > 0) it.sizeBytes else 0L }
    val maxEnd = tab.network.maxOfOrNull { it.startOffsetMs + it.durationMs.coerceAtLeast(0) } ?: 1

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${tab.network.size} requests", style = MaterialTheme.typography.labelMedium)
            Text(BrowserEngine.prettySize(totalBytes) + " transferred", style = MaterialTheme.typography.labelMedium)
            Text("${tab.blockedCount} blocked", style = MaterialTheme.typography.labelMedium, color = LogWarn)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Filter by URL", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
            )
            IconButton(onClick = {
                val har = tab.network.joinToString("\n") {
                    "${it.method} ${it.status} ${it.resourceType} ${BrowserEngine.prettySize(it.sizeBytes)} ${it.durationMs}ms ${it.url}"
                }
                clipboard.setText(AnnotatedString(har))
                vm.toast("Request log copied")
            }) { Icon(Icons.Default.ContentCopy, "Export log") }
            IconButton(onClick = { vm.clearNetwork(tab) }) { Icon(Icons.Default.Block, "Clear") }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(listOf("all", "doc", "xhr", "js", "css", "img", "blocked", "failed")) { id ->
                Chip(label = id.uppercase(), selected = filter == id, onClick = { filter = id })
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(records) { record ->
                NetworkRow(record, maxEnd) { detail = record }
            }
            if (records.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No requests captured", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Reload the page to record requests. Method and headers come from the network " +
                                "interceptor, sizes and timing from Resource Timing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    val selected = detail
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            Column(
                Modifier
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 26.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(selected.fileName, style = MaterialTheme.typography.titleMedium)
                CodeBlock(selected.url, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                DetailLine("Method", selected.method)
                DetailLine("Status", if (selected.blocked) "Blocked by content blocker" else "${selected.status} ${selected.statusText}")
                DetailLine("Type", selected.resourceType)
                DetailLine("Size", BrowserEngine.prettySize(selected.sizeBytes))
                DetailLine("Duration", if (selected.durationMs >= 0) "${selected.durationMs} ms" else "not reported")
                DetailLine("Start offset", "${selected.startOffsetMs} ms")
                DetailLine("From cache", if (selected.fromCache) "yes" else "no")
                Spacer(Modifier.height(10.dp))
                Text("Request headers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (selected.requestHeaders.isEmpty()) {
                    CodeBlock("Not exposed by the engine for this request", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                selected.requestHeaders.forEach { (k, v) -> CodeBlock("$k: $v") }
                if (selected.responseHeaders.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Response headers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    selected.responseHeaders.forEach { (k, v) -> CodeBlock("$k: $v") }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(selected.url))
                        vm.toast("URL copied")
                    }) { Text("Copy URL") }
                    TextButton(onClick = {
                        clipboard.setText(
                            AnnotatedString(
                                "curl '${selected.url}' " +
                                    selected.requestHeaders.entries.joinToString(" ") { "-H '${it.key}: ${it.value}'" },
                            ),
                        )
                        vm.toast("cURL command copied")
                    }) { Text("Copy as cURL") }
                    TextButton(onClick = { vm.startDownload(selected.url) }) { Text("Download") }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NetworkRow(record: NetworkRecord, maxEnd: Int, onClick: () -> Unit) {
    val statusColor = when {
        record.blocked -> LogWarn
        record.status >= 400 -> LogError
        record.status in 300..399 -> LogInfo
        record.status in 200..299 -> LogSuccess
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                record.resourceType.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(42.dp),
            )
            CodeBlock(record.fileName.ifBlank { record.url }, maxLines = 1, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            Text(
                if (record.blocked) "blocked" else if (record.status == 0) "\u2014" else "${record.status}",
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                BrowserEngine.prettySize(record.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (record.durationMs > 0) {
            val fraction = (record.durationMs.toFloat() / maxEnd.coerceAtLeast(1)).coerceIn(0.01f, 1f)
            val offset = (record.startOffsetMs.toFloat() / maxEnd.coerceAtLeast(1)).coerceIn(0f, 0.95f)
            Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
                Spacer(Modifier.weight(offset.coerceAtLeast(0.001f)))
                Box(
                    Modifier
                        .weight(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.weight((1f - offset - fraction).coerceAtLeast(0.001f)))
                Text(
                    "${record.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ Storage */

@Composable
private fun StoragePanel(vm: BrowserViewModel, tab: TabState) {
    var area by remember { mutableStateOf("local") }
    var entries by remember { mutableStateOf<List<StorageEntry>>(emptyList()) }
    var editing by remember { mutableStateOf<StorageEntry?>(null) }

    fun refresh() {
        vm.evalString("window.__db_storage ? window.__db_storage() : '{}';", tab) { payload ->
            val out = mutableListOf<StorageEntry>()
            try {
                val obj = devJson.parseToJsonElement(payload).jsonObject
                listOf("local", "session", "cookies").forEach { key ->
                    obj[key]?.jsonArray?.forEach { pair ->
                        val arr = pair.jsonArray
                        out.add(
                            StorageEntry(
                                key,
                                arr.getOrNull(0)?.jsonPrimitive?.content ?: "",
                                arr.getOrNull(1)?.jsonPrimitive?.content ?: "",
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            vm.cookiesFor(tab.url).forEach { (k, v) ->
                if (out.none { it.area == "cookies" && it.key == k }) out.add(StorageEntry("cookies", k, v))
            }
            entries = out
        }
    }

    LaunchedEffect(tab.id) { refresh() }

    val shown = entries.filter { it.area == area }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chip("Local Storage", area == "local") { area = "local" }
            Chip("Session", area == "session") { area = "session" }
            Chip("Cookies", area == "cookies") { area = "cookies" }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${shown.size} entries", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = {
                vm.evalRaw("window.__db_storage_clear && window.__db_storage_clear('$area');", tab) { refresh() }
                vm.toast("Cleared $area storage")
            }) { Text("Clear $area") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(shown) { entry ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editing = entry }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CodeBlock(entry.key, color = CodeAttr, maxLines = 1, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                vm.evalRaw(
                                    "window.__db_storage_del && window.__db_storage_del('${entry.area}'," +
                                        "${BrowserEngine.jsStringLiteral(entry.key)});",
                                    tab,
                                ) { refresh() }
                            },
                            modifier = Modifier.size(28.dp),
                        ) { Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp)) }
                    }
                    CodeBlock(entry.value, color = CodeValue, maxLines = 3)
                }
            }
            if (shown.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No entries in $area storage", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "IndexedDB and Cache Storage contents are not exposed by the WebView engine; " +
                                "they can be cleared from Settings \u203a Clear data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    val target = editing
    if (target != null) {
        var value by remember(target) { mutableStateOf(target.value) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(target.key) },
            text = {
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value") })
            },
            confirmButton = {
                TextButton(
                    enabled = target.area != "cookies",
                    onClick = {
                        vm.evalRaw(
                            "window.__db_storage_set && window.__db_storage_set('${target.area}'," +
                                "${BrowserEngine.jsStringLiteral(target.key)},${BrowserEngine.jsStringLiteral(value)});",
                            tab,
                        ) { refresh() }
                        editing = null
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Close") } },
        )
    }
}

/* ------------------------------------------------------------------ Performance */

@Composable
private fun PerformancePanel(vm: BrowserViewModel, tab: TabState) {
    var metrics by remember { mutableStateOf<List<PerfMetric>>(emptyList()) }

    fun refresh() {
        vm.evalString("window.__db_perf ? window.__db_perf() : '{}';", tab) { payload ->
            val out = mutableListOf<PerfMetric>()
            try {
                val obj = devJson.parseToJsonElement(payload).jsonObject
                fun num(key: String): String? = obj[key]?.jsonPrimitive?.content
                fun add(label: String, key: String, group: String, suffix: String = " ms") {
                    val value = num(key) ?: return
                    out.add(PerfMetric(label, value + suffix, group))
                }
                add("DNS lookup", "dns", "Navigation timing")
                add("TCP connect", "connect", "Navigation timing")
                add("TTFB", "ttfb", "Navigation timing")
                add("Content download", "download", "Navigation timing")
                add("DOM interactive", "domInteractive", "Navigation timing")
                add("DOM complete", "domComplete", "Navigation timing")
                add("Load event", "loadEvent", "Navigation timing")
                add("First paint", "firstPaint", "Rendering")
                add("First contentful paint", "fcp", "Rendering")
                num("protocol")?.let { out.add(PerfMetric("Protocol", it, "Navigation timing")) }
                num("resourceCount")?.let { out.add(PerfMetric("Resources", it, "Resources", "")) }
                num("resourceBytes")?.let {
                    out.add(PerfMetric("Transferred", BrowserEngine.prettySize(it.toLongOrNull() ?: 0L), "Resources"))
                }
                num("domNodes")?.let { out.add(PerfMetric("DOM nodes", it, "DOM")) }
                num("scripts")?.let { out.add(PerfMetric("Scripts", it, "DOM")) }
                num("stylesheets")?.let { out.add(PerfMetric("Stylesheets", it, "DOM")) }
                num("images")?.let { out.add(PerfMetric("Images", it, "DOM")) }
                num("iframes")?.let { out.add(PerfMetric("Iframes", it, "DOM")) }
                num("longTasks")?.let { out.add(PerfMetric("Long tasks", it, "Rendering")) }
                val heap = num("heapUsed")
                if (heap != null) {
                    out.add(PerfMetric("JS heap used", BrowserEngine.prettySize(heap.toLongOrNull() ?: 0L), "Memory"))
                    num("heapTotal")?.let {
                        out.add(PerfMetric("JS heap total", BrowserEngine.prettySize(it.toLongOrNull() ?: 0L), "Memory"))
                    }
                    num("heapLimit")?.let {
                        out.add(PerfMetric("Heap limit", BrowserEngine.prettySize(it.toLongOrNull() ?: 0L), "Memory"))
                    }
                } else {
                    out.add(PerfMetric("JS heap", "not exposed by engine", "Memory"))
                }
            } catch (e: Exception) {
                out.add(PerfMetric("Error", "Could not read performance data", "Navigation timing"))
            }
            metrics = out
        }
    }

    LaunchedEffect(tab.id) { refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { refresh() }) { Text("Capture metrics") }
            TextButton(onClick = { vm.measureFps(tab) }) { Text("Measure FPS") }
            TextButton(onClick = {
                vm.reload(tab)
                vm.toast("Reloading and recording")
            }) { Text("Reload & record") }
        }
        if (vm.lastFps >= 0) {
            Card(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("${vm.lastFps} FPS", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Measured over 1 second with requestAnimationFrame",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        metrics.groupBy { it.group }.forEach { (group, items) ->
            Text(
                group,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
            )
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    items.forEach { metric ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(metric.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                metric.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
        if (metrics.isEmpty()) {
            Text(
                "Tap Capture metrics to read Navigation Timing, Resource Timing and memory data " +
                    "from the live page.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

/* ------------------------------------------------------------------ Sources */

@Composable
private fun SourcesPanel(vm: BrowserViewModel, tab: TabState) {
    var source by remember(tab.id) { mutableStateOf("") }
    var assets by remember(tab.id) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var viewing by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(tab.id) {
        vm.evalString("window.__db_source ? window.__db_source() : '';", tab) { source = it }
        vm.evalString("window.__db_assets ? window.__db_assets() : '[]';", tab) { payload ->
            assets = try {
                devJson.parseToJsonElement(payload).jsonArray.map {
                    val obj = it.jsonObject
                    (obj["url"]?.jsonPrimitive?.content ?: "") to (obj["type"]?.jsonPrimitive?.content ?: "")
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip("Page source", viewing == null) { viewing = null }
            Chip("Format", false) {
                source = BrowserEngine.formatMarkup(source)
                vm.toast("HTML formatted")
            }
            Chip("Copy", false) {
                clipboard.setText(AnnotatedString(if (viewing == null) source else fileContent))
                vm.toast("Source copied")
            }
        }
        if (viewing == null) {
            LazyColumn(Modifier.weight(1f)) {
                item {
                    Text(
                        "Linked resources (${assets.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                items(assets.filter { it.second == "script" || it.second == "stylesheet" }) { (url, type) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewing = url
                                loading = true
                                scope.launch {
                                    val text = withContext(Dispatchers.IO) {
                                        try {
                                            val connection = java.net.URL(url).openConnection()
                                            connection.connectTimeout = 8000
                                            connection.readTimeout = 8000
                                            connection.getInputStream().bufferedReader().use { it.readText() }
                                        } catch (e: Exception) {
                                            "Could not fetch resource: ${e.message}"
                                        }
                                    }
                                    fileContent = if (url.endsWith(".css")) BrowserEngine.formatCss(text) else text
                                    loading = false
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            type.uppercase().take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(34.dp),
                        )
                        CodeBlock(url.substringAfterLast('/').ifBlank { url }, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Text(
                        "Document",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                item {
                    Box(Modifier.horizontalScroll(rememberScrollState())) {
                        CodeBlock(
                            source.ifBlank { "Loading document source\u2026" },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CodeBlock(viewing ?: "", maxLines = 1, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewing = null }) { Icon(Icons.Default.Close, "Close file") }
                }
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Box(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    CodeBlock(fileContent, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}
