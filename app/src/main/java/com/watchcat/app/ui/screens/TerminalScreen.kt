package com.watchcat.app.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchcat.app.ui.components.GlassActionButton
import com.watchcat.app.ui.components.GlassCard
import com.watchcat.app.ui.components.GlassIconButton
import com.watchcat.app.ui.components.GlassTopBar
import com.watchcat.app.ui.theme.AccentCyan
import com.watchcat.app.ui.theme.AccentEmerald
import com.watchcat.app.ui.theme.AccentRose
import com.watchcat.app.ui.theme.AppThemeState
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.Ink
import com.watchcat.app.ui.theme.PageBackground
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.ui.theme.TextSecondary
import com.watchcat.app.ui.theme.TextTertiary
import com.watchcat.app.ui.theme.accentAt
import com.watchcat.app.vm.AppViewModel
import com.watchcat.app.vm.TerminalSession
import com.watchcat.app.vm.TerminalState
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Composable
fun TerminalScreen(
    viewModel: AppViewModel,
    serverId: String,
    onBack: () -> Unit,
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val server = servers.firstOrNull { it.id == serverId }
    val context = LocalContext.current
    val view = LocalView.current

    if (server == null || server.isDemo) {
        Column(Modifier.fillMaxSize().background(PageBackground)) {
            GlassTopBar(
                title = "无法打开终端",
                subtitle = if (server?.isDemo == true) "演示服务器不支持 SSH" else "服务器不存在",
                leading = {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                    )
                },
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("返回上一页后选择真实服务器", color = TextSecondary)
            }
        }
        return
    }

    val dark = AppThemeState.isDark
    val session = remember(server.id) { TerminalSession(viewModel, server.id) }
    val termState by session.state.collectAsStateWithLifecycle()
    val termError by session.error.collectAsStateWithLifecycle()
    val accent = accentAt(server.accentIndex)
    val clipboard = remember(context) { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val holder = remember { WebViewHolder() }
    val lastCols = remember { AtomicInteger(80) }
    val lastRows = remember { AtomicInteger(24) }
    val selection = remember { AtomicReference("") }
    var jsReady by remember { mutableStateOf(false) }

    val bridge = remember(session, context) {
        TerminalBridge(
            inputCallback = { text -> session.write(text) },
            resizeCallback = { cols, rows ->
                lastCols.set(cols)
                lastRows.set(rows)
                session.resize(cols, rows)
            },
            readyCallback = { mainHandler.post { jsReady = true } },
            selectionCallback = { text -> selection.set(text) },
            urlCallback = { url ->
                mainHandler.post {
                    runCatching {
                        val uri = Uri.parse(url)
                        if (uri.scheme == "http" || uri.scheme == "https") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    }
                }
            },
        )
    }

    // 仅在终端页保持屏幕常亮
    DisposableEffect(view) {
        val activity = view.context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(session) {
        onDispose { session.dispose() }
    }

    LaunchedEffect(jsReady) {
        if (jsReady) {
            session.connect(lastCols.get(), lastRows.get())
        }
    }

    LaunchedEffect(termState) {
        if (termState == TerminalState.CONNECTING) {
            holder.view?.evaluateJavascript("window.termReset()", null)
        }
    }

    LaunchedEffect(session) {
        session.output.collect { text ->
            val webView = holder.view ?: return@collect
            webView.evaluateJavascript("window.termWrite(${JSONObject.quote(text)})", null)
        }
    }

    LaunchedEffect(settings.terminalFontSize) {
        holder.view?.evaluateJavascript(
            "if (window.termSetFontSize) window.termSetFontSize(${settings.terminalFontSize})",
            null,
        )
    }

    fun showKeyboard() {
        val webView = holder.view ?: return
        webView.requestFocus()
        webView.evaluateJavascript("window.termFocus()", null)
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
    }

    fun copySelection() {
        val webView = holder.view ?: return
        webView.evaluateJavascript("window.termGetSelection()") { raw ->
            val text = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
                .orEmpty()
                .ifBlank { selection.get() }
            if (text.isBlank()) {
                viewModel.toast("先长按终端选择要复制的文本")
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("终端文本", text))
                viewModel.toast("已复制终端文本")
            }
        }
    }

    fun paste() {
        val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
        if (text.isEmpty()) {
            viewModel.toast("剪贴板为空")
        } else {
            session.write(text)
        }
    }

    val stateLabel = when (termState) {
        TerminalState.CONNECTING -> "正在连接"
        TerminalState.CONNECTED -> "已连接"
        TerminalState.DISCONNECTED -> "会话已结束"
        TerminalState.FAILED -> "连接失败"
        TerminalState.IDLE -> "准备中"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (dark) {
                        listOf(
                            Color(0xFF05070E),
                            Color(0xFF070B18),
                            Color(0xFF04050B),
                        )
                    } else {
                        listOf(
                            Color(0xFFFDFDFE),
                            Color(0xFFF5F5F7),
                            Color(0xFFEDEDF1),
                        )
                    },
                ),
            )
            .imePadding(),
    ) {
        GlassTopBar(
            title = server.name,
            subtitle = "SSH 终端 · $stateLabel",
            leading = {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                )
            },
            actions = {
                GlassIconButton(
                    icon = Icons.Rounded.Keyboard,
                    contentDescription = "键盘",
                    onClick = { showKeyboard() },
                    tint = AccentCyan,
                )
                GlassIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    contentDescription = "复制选中文本",
                    onClick = { copySelection() },
                )
                GlassIconButton(
                    icon = Icons.Rounded.RestartAlt,
                    contentDescription = "重新连接",
                    onClick = { session.connect(lastCols.get(), lastRows.get()) },
                    tint = if (termState == TerminalState.FAILED) AccentRose else TextSecondary,
                )
            },
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        overScrollMode = View.OVER_SCROLL_NEVER
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()
                        this.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = false
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            loadWithOverviewMode = false
                            useWideViewPort = false
                            textZoom = 100
                            mediaPlaybackRequiresUserGesture = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        addJavascriptInterface(bridge, "Bridge")
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                android.util.Log.d(
                                    "WatchCatTerm",
                                    "${message.message()} (${message.lineNumber()})",
                                )
                                return true
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                view.postDelayed({
                                    view.evaluateJavascript("if (window.termFit) window.termFit()", null)
                                }, 250)
                                view.postDelayed({
                                    view.evaluateJavascript("if (window.termFit) window.termFit()", null)
                                }, 800)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val url = request.url.toString()
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                        holder.view = this
                        val hex = "#" + (accent.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')
                        loadUrl(
                            "file:///android_asset/terminal/index.html" +
                                "?fontSize=${settings.terminalFontSize}&accent=${Uri.encode(hex)}&dark=${if (dark) 1 else 0}",
                        )
                    }
                },
                update = { webView ->
                    webView.evaluateJavascript(
                        "if (window.termSetFontSize) window.termSetFontSize(${settings.terminalFontSize}); if (window.termFit) window.termFit();",
                        null,
                    )
                },
                onRelease = { webView ->
                    webView.removeJavascriptInterface("Bridge")
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.destroy()
                    if (holder.view === webView) holder.view = null
                },
            )

            when (termState) {
                TerminalState.CONNECTING -> TerminalOverlay(
                    title = "正在建立 SSH 会话",
                    message = "${server.username}@${server.displayHost}",
                    accent = AccentCyan,
                    showSpinner = true,
                    onRetry = null,
                )

                TerminalState.FAILED -> TerminalOverlay(
                    title = "连接失败",
                    message = termError ?: "无法建立 SSH 会话",
                    accent = AccentRose,
                    onRetry = { session.connect(lastCols.get(), lastRows.get()) },
                )

                TerminalState.DISCONNECTED -> TerminalOverlay(
                    title = "会话已结束",
                    message = "远程 shell 已关闭或连接中断。",
                    accent = AccentRose,
                    onRetry = { session.connect(lastCols.get(), lastRows.get()) },
                )

                else -> Unit
            }

        }

        ExtraKeysRow(
            onKey = { sequence -> session.write(sequence) },
            onPaste = { paste() },
        )
    }
}

@Composable
private fun TerminalOverlay(
    title: String,
    message: String,
    accent: Color,
    onRetry: (() -> Unit)?,
    showSpinner: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground.copy(alpha = 0.72f))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            accent = accent,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showSpinner) {
                    CircularProgressIndicator(
                        color = accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                GlassActionButton(
                    text = "重新连接",
                    icon = Icons.Rounded.RestartAlt,
                    accent = accent,
                    primary = true,
                    onClick = onRetry,
                )
            }
        }
    }
}

@Composable
private fun ExtraKeysRow(
    onKey: (String) -> Unit,
    onPaste: () -> Unit,
) {
    val keys = remember {
        listOf(
            "Esc" to "\u001b",
            "Tab" to "\t",
            "^C" to "\u0003",
            "^D" to "\u0004",
            "↑" to "\u001b[A",
            "↓" to "\u001b[B",
            "←" to "\u001b[D",
            "→" to "\u001b[C",
            "|" to "|",
            "~" to "~",
            "/" to "/",
            "-" to "-",
        )
    }
    val dark = AppThemeState.isDark
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    if (dark) {
                        listOf(Color(0xFF070A14), Color(0xFF0A0E1C))
                    } else {
                        listOf(Color(0xFFF9F9FB), Color(0xFFEFEFF3))
                    },
                ),
            )
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { (label, sequence) ->
            TerminalKey(label = label, onClick = { onKey(sequence) })
        }
        TerminalKey(label = "Paste", accent = AccentCyan, onClick = onPaste)
    }
}

@Composable
private fun TerminalKey(
    label: String,
    accent: Color = TextSecondary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(GlassLine.copy(alpha = 0.055f))
            .border(1.dp, GlassLine.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = accent,
            maxLines = 1,
        )
    }
}

private class WebViewHolder {
    var view: WebView? = null
}

class TerminalBridge(
    private val inputCallback: (String) -> Unit,
    private val resizeCallback: (Int, Int) -> Unit,
    private val readyCallback: () -> Unit,
    private val selectionCallback: (String) -> Unit,
    private val urlCallback: (String) -> Unit,
) {
    @JavascriptInterface
    fun onInput(data: String) = inputCallback(data)

    @JavascriptInterface
    fun onResize(cols: Int, rows: Int) = resizeCallback(cols, rows)

    @JavascriptInterface
    fun onReady() = readyCallback()

    @JavascriptInterface
    fun onSelection(text: String) = selectionCallback(text)

    @JavascriptInterface
    fun openUrl(url: String) = urlCallback(url)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
