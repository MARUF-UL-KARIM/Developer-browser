package com.agon.app.ui.screens

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.agon.app.browser.TabState
import com.agon.app.data.hostOf
import com.agon.app.ui.components.Chip
import com.agon.app.ui.components.Favicon
import com.agon.app.viewmodel.BrowserViewModel

@Composable
fun BrowserScreen(
    vm: BrowserViewModel,
    onRoute: (String) -> Unit,
) {
    val context = LocalContext.current
    val tab = vm.activeTab
    var omniboxOpen by remember { mutableStateOf(false) }
    var omniboxText by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var findOpen by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var siteInfoOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.toastMessage) {
        val message = vm.toastMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.consumeToast()
        }
    }

    BackHandler(enabled = true) {
        when {
            omniboxOpen -> omniboxOpen = false
            findOpen -> { findOpen = false; vm.findInPage("") }
            vm.fullScreen -> vm.fullScreen = false
            tab?.webView?.canGoBack() == true -> vm.goBack(tab)
            tab?.isHome == false -> vm.goHome(tab)
            else -> (context as? Activity)?.finish()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (!vm.fullScreen) {
                    AddressBar(
                        vm = vm,
                        tab = tab,
                        onOpenOmnibox = {
                            omniboxText = if (tab?.isHome == true) "" else (tab?.url ?: "")
                            omniboxOpen = true
                        },
                        onSiteInfo = { siteInfoOpen = true },
                        onDevTools = { onRoute("devtools") },
                    )
                    val progress by animateFloatAsState(tab?.progress ?: 0f, label = "progress")
                    AnimatedVisibility(
                        visible = tab?.loading == true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                        )
                    }
                }

                Box(Modifier.weight(1f)) {
                    if (tab == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (tab.isHome) {
                        HomePage(
                            vm = vm,
                            onOmnibox = { omniboxText = ""; omniboxOpen = true },
                            onOpenEngines = { onRoute("settings") },
                            onNavigate = { vm.navigate(it, tab) },
                            onOpenRoute = onRoute,
                        )
                    } else {
                        WebViewHost(vm, tab, Modifier.fillMaxSize())
                        if (tab.crashed) {
                            ErrorOverlay(tab.url) { vm.reload(tab) }
                        }
                    }

                    if (findOpen) {
                        FindBar(
                            vm = vm,
                            query = findText,
                            onQuery = { findText = it; vm.findInPage(it, tab) },
                            onClose = { findOpen = false; findText = ""; vm.findInPage("", tab) },
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }

                if (!vm.fullScreen) {
                    BottomBar(
                        vm = vm,
                        tab = tab,
                        onTabs = { vm.captureThumbnail(tab); onRoute("tabs") },
                        onMenu = { menuOpen = true },
                    )
                }
            }

            if (vm.fullScreen) {
                IconButton(
                    onClick = { vm.fullScreen = false },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                ) {
                    Icon(Icons.Default.Close, "Exit full screen", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            if (omniboxOpen) {
                Omnibox(
                    vm = vm,
                    text = omniboxText,
                    onText = { omniboxText = it },
                    onSubmit = {
                        if (it.isNotBlank()) {
                            vm.navigate(it, tab)
                            omniboxOpen = false
                        }
                    },
                    onClose = { omniboxOpen = false },
                )
            }
        }
    }

    if (menuOpen) {
        MainMenuSheet(
            vm = vm,
            tab = tab,
            onDismiss = { menuOpen = false },
            onRoute = onRoute,
            onFind = { findOpen = true },
            onSiteInfo = { siteInfoOpen = true },
        )
    }

    if (siteInfoOpen) {
        SiteInfoSheet(vm = vm, tab = tab, onDismiss = { siteInfoOpen = false })
    }
}

@Composable
fun WebViewHost(vm: BrowserViewModel, tab: TabState, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { container ->
            val webView: WebView = vm.obtainWebView(container.context, tab)
            if (webView.parent !== container) {
                (webView.parent as? ViewGroup)?.removeView(webView)
                container.removeAllViews()
                container.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
    )
}

@Composable
private fun AddressBar(
    vm: BrowserViewModel,
    tab: TabState?,
    onOpenOmnibox: () -> Unit,
    onSiteInfo: () -> Unit,
    onDevTools: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onOpenOmnibox,
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
            ) {
                Row(
                    Modifier.padding(start = 6.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onSiteInfo, modifier = Modifier.size(32.dp)) {
                        val incognito = tab?.incognito == true
                        Icon(
                            when {
                                incognito -> Icons.Default.VisibilityOff
                                tab?.secure == true -> Icons.Default.Lock
                                tab?.isHome == true -> Icons.Default.Search
                                else -> Icons.Default.LockOpen
                            },
                            contentDescription = "Site information",
                            modifier = Modifier.size(17.dp),
                            tint = if (tab?.secure == true) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            tab == null || tab.isHome -> "Search or type URL"
                            else -> hostOf(tab.url).ifBlank { tab.url }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = if (tab?.isHome != false) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    if (tab?.desktopMode == true) {
                        Text(
                            "DESKTOP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = { if (tab?.loading == true) vm.stopLoading(tab) else vm.reload(tab) },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            if (tab?.loading == true) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = "Reload",
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
            IconButton(onClick = onDevTools) {
                Box {
                    Icon(Icons.Default.Build, contentDescription = "DevTools")
                    val count = tab?.console?.count { it.level == "error" } ?: 0
                    if (count > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (count > 9) "9+" else "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    vm: BrowserViewModel,
    tab: TabState?,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.goBack(tab) }, enabled = tab?.canGoBack == true || tab?.isHome == false) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            IconButton(onClick = { vm.goForward(tab) }, enabled = tab?.canGoForward == true) {
                Icon(Icons.Default.ArrowForward, "Forward")
            }
            IconButton(onClick = { vm.goHome(tab) }) {
                Icon(Icons.Default.Home, "Home")
            }
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = onTabs) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${vm.tabs.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.MoreVert, "Menu")
            }
        }
    }
}

@Composable
private fun Omnibox(
    vm: BrowserViewModel,
    text: String,
    onText: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val clipboardUrl = remember {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val item = manager?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        item?.takeIf { it.startsWith("http") }
    }
    val suggestions = vm.suggestions(text)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Close") }
                OutlinedTextField(
                    value = text,
                    onValueChange = onText,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                    placeholder = { Text("Search ${vm.searchEngine.name} or type a URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = { onText("") }) { Icon(Icons.Default.Close, "Clear") }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit(text) }),
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.engines) { engine ->
                    Chip(
                        label = engine.name,
                        selected = engine.id == vm.settings.searchEngineId,
                        onClick = { vm.setSearchEngine(engine.id) },
                    )
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                if (text.isNotBlank()) {
                    item {
                        SuggestionRow(
                            icon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            title = if (vm.isProbablyUrl(text)) text else "$text \u2014 ${vm.searchEngine.name} search",
                            subtitle = if (vm.isProbablyUrl(text)) "Open website" else "Search the web",
                            onClick = { onSubmit(text) },
                        )
                    }
                }
                if (clipboardUrl != null && text.isBlank()) {
                    item {
                        SuggestionRow(
                            icon = { Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.secondary) },
                            title = clipboardUrl,
                            subtitle = "Link from clipboard",
                            onClick = { onSubmit(clipboardUrl) },
                        )
                    }
                }
                items(suggestions) { (title, url) ->
                    SuggestionRow(
                        icon = { Favicon(url, size = 26.dp, corner = 13.dp) },
                        title = title.ifBlank { url },
                        subtitle = url,
                        onClick = { onSubmit(url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(14.dp))
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
        }
    }
}

@Composable
private fun FindBar(
    vm: BrowserViewModel,
    query: String,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                placeholder = { Text("Find in page") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Text(
                "${vm.findIndex}/${vm.findMatches}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = { vm.findNext(false) }) { Icon(Icons.Default.KeyboardArrowUp, "Previous") }
            IconButton(onClick = { vm.findNext(true) }) { Icon(Icons.Default.KeyboardArrowDown, "Next") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }
    }
}

@Composable
private fun ErrorOverlay(url: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Warning, null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(46.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text("This page could not be loaded", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            hostOf(url),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}

fun lockOrientation(context: Context, mode: Int) {
    (context as? Activity)?.requestedOrientation = when (mode) {
        1 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        2 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
