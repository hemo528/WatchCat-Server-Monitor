package com.watchcat.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchcat.app.data.AuthType
import com.watchcat.app.data.ServerConfig
import com.watchcat.app.ssh.ConnectionTestResult
import com.watchcat.app.ui.components.GlassActionButton
import com.watchcat.app.ui.components.GlassCard
import com.watchcat.app.ui.components.GlassDivider
import com.watchcat.app.ui.components.GlassIconButton
import com.watchcat.app.ui.components.GlassPill
import com.watchcat.app.ui.components.GlassTopBar
import com.watchcat.app.ui.components.SectionLabel
import com.watchcat.app.ui.components.formatDurationShort
import com.watchcat.app.ui.components.lerpColor
import com.watchcat.app.ui.theme.AccentCyan
import com.watchcat.app.ui.theme.AccentEmerald
import com.watchcat.app.ui.theme.AccentRose
import com.watchcat.app.ui.theme.AccentViolet
import com.watchcat.app.ui.theme.AccentPalette
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.OnAccent
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.ui.theme.TextSecondary
import com.watchcat.app.ui.theme.TextTertiary
import com.watchcat.app.ui.theme.accentAt
import com.watchcat.app.vm.AppViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EditServerScreen(
    viewModel: AppViewModel,
    serverId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val existing = servers.firstOrNull { it.id == serverId }
    val isNew = serverId == "new"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var initialized by rememberSaveable(serverId) { mutableStateOf(isNew) }

    var name by rememberSaveable(serverId) { mutableStateOf("") }
    var host by rememberSaveable(serverId) { mutableStateOf("") }
    var port by rememberSaveable(serverId) { mutableStateOf("22") }
    var username by rememberSaveable(serverId) { mutableStateOf("root") }
    var authType by rememberSaveable(serverId) { mutableStateOf(AuthType.PASSWORD) }
    // 敏感字段只保存在内存中，避免进入 savedInstanceState
    var password by remember(serverId) { mutableStateOf("") }
    var privateKey by remember(serverId) { mutableStateOf("") }
    var passphrase by remember(serverId) { mutableStateOf("") }
    var accentIndex by rememberSaveable(serverId) { mutableIntStateOf(0) }
    var fingerprint by rememberSaveable(serverId) { mutableStateOf<String?>(null) }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var keyVisible by rememberSaveable { mutableStateOf(true) }
    var passphraseVisible by rememberSaveable { mutableStateOf(false) }

    var testing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var testError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existing, isNew) {
        if (!initialized && existing != null) {
            name = existing.name
            host = existing.host
            port = existing.port.toString()
            username = existing.username
            authType = existing.authType
            accentIndex = existing.accentIndex
            fingerprint = existing.fingerprint
            initialized = true
        }
    }

    if (!isNew && existing == null) {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "服务器不存在",
                subtitle = "它可能已经被删除",
                leading = {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                    )
                },
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("找不到这台服务器", color = TextSecondary)
            }
        }
        return
    }

    val accent = accentAt(accentIndex)

    fun buildCandidate(): ServerConfig? {
        val cleanHost = host.trim()
            .removePrefix("ssh://")
            .removeSuffix("/")
        if (cleanHost.isBlank()) {
            formError = "请填写服务器地址"
            return null
        }
        val portNumber = port.trim().toIntOrNull()
        if (portNumber == null || portNumber !in 1..65535) {
            formError = "端口必须在 1 - 65535 之间"
            return null
        }
        if (username.isBlank()) {
            formError = "请填写登录用户名"
            return null
        }
        if (isNew || existing?.authType != authType) {
            if (authType == AuthType.PASSWORD && password.isEmpty()) {
                formError = "请填写 SSH 密码"
                return null
            }
            if (authType == AuthType.PRIVATE_KEY && privateKey.isBlank()) {
                formError = "请粘贴 SSH 私钥"
                return null
            }
        }
        formError = null
        return ServerConfig(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { cleanHost },
            host = cleanHost,
            port = portNumber,
            username = username.trim(),
            authType = authType,
            accentIndex = accentIndex,
            fingerprint = fingerprint,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
    }

    fun runTest() {
        val candidate = buildCandidate() ?: return
        testing = true
        testResult = null
        testError = null
        scope.launch {
            viewModel.testDraft(candidate, password, privateKey, passphrase)
                .onSuccess { result ->
                    testResult = result
                    fingerprint = result.fingerprint.takeIf { it.startsWith("SHA256:") } ?: fingerprint
                }
                .onFailure { error ->
                    testError = viewModel.humanError(error)
                }
            testing = false
        }
    }

    fun save() {
        val candidate = buildCandidate() ?: return
        saving = true
        scope.launch {
            val replaceSecrets = isNew || existing?.authType != authType
            viewModel.saveServer(
                config = candidate,
                password = password,
                privateKey = privateKey,
                passphrase = passphrase,
                replaceSecrets = replaceSecrets,
            ).onSuccess {
                saving = false
                viewModel.toast("服务器已保存")
                onSaved()
            }.onFailure { error ->
                saving = false
                formError = error.message ?: "保存失败"
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = if (isNew) "添加服务器" else "编辑服务器",
                subtitle = if (isNew) "通过 SSH 读取状态" else existing?.displayHost,
                leading = {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                    )
                },
                actions = {
                    GlassIconButton(
                        icon = Icons.Rounded.Check,
                        contentDescription = "保存",
                        onClick = { if (!saving) save() },
                        tint = if (saving) TextTertiary else AccentEmerald,
                    )
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f).imePadding().navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = accent,
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        SectionLabel("基本信息")
                        Spacer(Modifier.height(14.dp))
                        FormField(
                            value = name,
                            onValueChange = { name = it },
                            label = "显示名称",
                            placeholder = "例如：Tokyo-01",
                            accent = accent,
                        )
                        Spacer(Modifier.height(11.dp))
                        FormField(
                            value = host,
                            onValueChange = {
                                if (it != host) {
                                    fingerprint = null
                                    testResult = null
                                }
                                host = it
                            },
                            label = "服务器地址",
                            placeholder = "192.168.1.10 或 example.com",
                            accent = accent,
                            keyboardType = KeyboardType.Uri,
                        )
                        Spacer(Modifier.height(11.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FormField(
                                value = port,
                                onValueChange = {
                                    port = it.filter { ch -> ch.isDigit() }.take(5)
                                    fingerprint = null
                                    testResult = null
                                },
                                label = "端口",
                                placeholder = "22",
                                accent = accent,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(0.38f),
                            )
                            FormField(
                                value = username,
                                onValueChange = { username = it },
                                label = "用户名",
                                placeholder = "root",
                                accent = accent,
                                modifier = Modifier.weight(0.62f),
                            )
                        }
                        Spacer(Modifier.height(11.dp))
                        FormField(
                            value = "${username.ifBlank { "root" }}@${host.ifBlank { "your-server" }}:${port.ifBlank { "22" }}",
                            onValueChange = {},
                            label = "连接预览",
                            accent = accent,
                            readOnly = true,
                            enabled = false,
                        )
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = AccentViolet,
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionLabel("认证方式")
                            GlassPill(
                                text = if (authType == AuthType.PASSWORD) "密码" else "私钥",
                                color = AccentViolet,
                                icon = if (authType == AuthType.PASSWORD) Icons.Rounded.Lock else Icons.Rounded.Key,
                            )
                        }
                        Spacer(Modifier.height(13.dp))
                        AuthTypeToggle(
                            selected = authType,
                            onSelect = {
                                if (authType != it) {
                                    authType = it
                                    testResult = null
                                    testError = null
                                }
                            },
                            accent = accent,
                        )
                        Spacer(Modifier.height(14.dp))

                        AnimatedVisibility(
                            visible = authType == AuthType.PASSWORD,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column {
                                FormField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = "SSH 密码",
                                    placeholder = if (!isNew && existing?.authType == AuthType.PASSWORD) "已保存，留空保持不变" else "输入密码",
                                    accent = accent,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardType = KeyboardType.Password,
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                                tint = TextTertiary,
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = authType == AuthType.PRIVATE_KEY,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column {
                                FormField(
                                    value = privateKey,
                                    onValueChange = { privateKey = it },
                                    label = "SSH 私钥",
                                    placeholder = if (!isNew && existing?.authType == AuthType.PRIVATE_KEY) "已保存，留空保持不变" else "-----BEGIN OPENSSH PRIVATE KEY-----",
                                    accent = accent,
                                    singleLine = false,
                                    minLines = 4,
                                    maxLines = 8,
                                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { keyVisible = !keyVisible }) {
                                            Icon(
                                                imageVector = if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = if (keyVisible) "隐藏私钥" else "显示私钥",
                                                tint = TextTertiary,
                                            )
                                        }
                                    },
                                )
                                Spacer(Modifier.height(11.dp))
                                FormField(
                                    value = passphrase,
                                    onValueChange = { passphrase = it },
                                    label = "私钥口令（可选）",
                                    placeholder = "没有口令可留空",
                                    accent = accent,
                                    visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardType = KeyboardType.Password,
                                    trailingIcon = {
                                        IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                                            Icon(
                                                imageVector = if (passphraseVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = if (passphraseVisible) "隐藏口令" else "显示口令",
                                                tint = TextTertiary,
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "支持密码、OpenSSH / PEM 私钥（RSA、ECDSA、Ed25519）。",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                            )
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = accent,
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        SectionLabel("卡片强调色")
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AccentPalette.forEachIndexed { index, color ->
                                val selected = accentIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(color, lerpColor(color, Color.Black, 0.35f)),
                                            ),
                                        )
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) Color.White.copy(alpha = 0.85f) else GlassLine.copy(alpha = 0.16f),
                                            shape = CircleShape,
                                        )
                                        .clickable { accentIndex = index },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = OnAccent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accent),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "用于卡片、头像与详情页的高亮配色",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                            )
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = if (testResult != null) AccentEmerald else accent,
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        SectionLabel("连接测试")
                        Spacer(Modifier.height(12.dp))
                        GlassActionButton(
                            text = if (testing) "正在连接…" else "测试 SSH 连接",
                            icon = if (testing) null else Icons.Rounded.LockOpen,
                            accent = accent,
                            onClick = { if (!testing) runTest() },
                            enabled = !testing && !saving,
                        )

                        if (testing) {
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = accent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "正在协商密钥并验证凭据…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        val success = testResult
                        if (success != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "连接成功 · ${formatDurationShort(success.latencyMs)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = AccentEmerald,
                                    )
                                    Text(
                                        text = "主机密钥 ${success.hostKeyType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                    Row(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("主机指纹", success.fingerprint))
                                                viewModel.toast("已复制主机指纹")
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Fingerprint,
                                            contentDescription = null,
                                            tint = TextTertiary,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = success.fingerprint,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Rounded.ContentCopy,
                                            contentDescription = null,
                                            tint = TextTertiary,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }
                        }

                        if (testError != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = AccentRose,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = testError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentRose,
                                )
                            }
                        }

                        Spacer(Modifier.height(13.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "首次连接会记录主机指纹，之后指纹变化将阻止连接。",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                            )
                        }
                    }
                }

                item {
                    GlassDivider()
                }

                item {
                    formError?.let { error ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AccentRose.copy(alpha = 0.10f))
                                .border(1.dp, AccentRose.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = AccentRose,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentRose,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    GlassActionButton(
                        text = if (saving) "保存中…" else if (isNew) "保存并开始监控" else "保存修改",
                        icon = if (saving) null else Icons.Rounded.Check,
                        accent = accent,
                        primary = true,
                        enabled = !saving,
                        onClick = { if (!saving) save() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "凭据使用 Android Keystore 加密，仅保存在本机",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary.copy(alpha = 0.75f),
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        readOnly = readOnly,
        enabled = enabled,
        textStyle = textStyle.copy(color = if (enabled) TextPrimary else TextTertiary),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextTertiary,
            focusedContainerColor = GlassLine.copy(alpha = 0.045f),
            unfocusedContainerColor = GlassLine.copy(alpha = 0.025f),
            disabledContainerColor = GlassLine.copy(alpha = 0.015f),
            focusedBorderColor = accent.copy(alpha = 0.75f),
            unfocusedBorderColor = GlassLine.copy(alpha = 0.12f),
            disabledBorderColor = GlassLine.copy(alpha = 0.07f),
            cursorColor = accent,
            focusedLabelColor = accent,
            unfocusedLabelColor = TextTertiary,
            focusedPlaceholderColor = TextTertiary,
            unfocusedPlaceholderColor = TextTertiary,
            focusedTrailingIconColor = accent,
            unfocusedTrailingIconColor = TextTertiary,
        ),
    )
}

@Composable
private fun AuthTypeToggle(
    selected: AuthType,
    onSelect: (AuthType) -> Unit,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(GlassLine.copy(alpha = 0.045f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        AuthOption(
            label = "密码登录",
            icon = Icons.Rounded.Lock,
            active = selected == AuthType.PASSWORD,
            accent = accent,
            onClick = { onSelect(AuthType.PASSWORD) },
            modifier = Modifier.weight(1f),
        )
        AuthOption(
            label = "私钥登录",
            icon = Icons.Rounded.Key,
            active = selected == AuthType.PRIVATE_KEY,
            accent = accent,
            onClick = { onSelect(AuthType.PRIVATE_KEY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AuthOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (active) accent.copy(alpha = 0.30f) else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) accent else TextTertiary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) TextPrimary else TextTertiary,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
