package com.watchcat.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchcat.app.data.AppSettings
import com.watchcat.app.data.ThemeMode
import com.watchcat.app.ui.components.GlassActionButton
import com.watchcat.app.ui.components.GlassCard
import com.watchcat.app.ui.components.GlassIconButton
import com.watchcat.app.ui.components.GlassPill
import com.watchcat.app.ui.components.GlassTopBar
import com.watchcat.app.ui.components.SectionLabel
import com.watchcat.app.ui.components.formatOneDecimal
import com.watchcat.app.ui.components.formatRefreshSeconds
import com.watchcat.app.ui.theme.AccentAmber
import com.watchcat.app.ui.theme.AccentCyan
import com.watchcat.app.ui.theme.AccentEmerald
import com.watchcat.app.ui.theme.AccentRose
import com.watchcat.app.ui.theme.AccentViolet
import com.watchcat.app.ui.theme.AppThemeState
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.OnAccent
import com.watchcat.app.ui.theme.Ink
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.ui.theme.TextSecondary
import com.watchcat.app.ui.theme.TextTertiary
import com.watchcat.app.vm.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val demo = servers.firstOrNull { it.isDemo }

    fun update(transform: (AppSettings) -> AppSettings) {
        scope.launch {
            viewModel.updateSettings(transform(viewModel.settings.value))
                .onFailure { viewModel.toast("保存设置失败：${it.message ?: "未知错误"}") }
        }
    }

    val focusManager = LocalFocusManager.current
    var customRefresh by rememberSaveable {
        mutableStateOf(formatOneDecimal(settings.refreshSeconds))
    }
    var customRefreshError by remember { mutableStateOf<String?>(null) }
    val refreshInputPattern = remember { Regex("""^\d{0,3}([.,]\d{0,1})?$""") }

    LaunchedEffect(settings.refreshSeconds) {
        customRefresh = formatOneDecimal(settings.refreshSeconds)
        customRefreshError = null
    }

    fun selectRefresh(seconds: Double) {
        customRefreshError = null
        update { it.copy(refreshSeconds = seconds) }
    }

    fun applyCustomRefresh() {
        val parsed = customRefresh.trim().replace(',', '.').toDoubleOrNull()
        if (parsed == null || parsed < 1.0 || parsed > 300.0) {
            customRefreshError = "请输入 1.0 – 300.0 之间的秒数"
            return
        }
        val rounded = (parsed * 10.0).roundToInt() / 10.0
        customRefresh = formatOneDecimal(rounded)
        customRefreshError = null
        focusManager.clearFocus()
        update { it.copy(refreshSeconds = rounded) }
        viewModel.toast("刷新频率已设为 ${formatOneDecimal(rounded)} 秒")
    }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "设置",
            subtitle = "WatchCat · 1.0.1",
            leading = {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                )
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = TextPrimary,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("外观")
                        GlassPill(text = themeModeLabel(settings.themeMode), color = TextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "默认跟随系统；也可以手动固定为白色或黑色毛玻璃。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(13.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeModeOption(
                            label = "跟随系统",
                            icon = Icons.Rounded.BrightnessAuto,
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            accent = TextPrimary,
                            onClick = { update { it.copy(themeMode = ThemeMode.SYSTEM) } },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeModeOption(
                            label = "浅色",
                            icon = Icons.Rounded.LightMode,
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            accent = TextPrimary,
                            onClick = { update { it.copy(themeMode = ThemeMode.LIGHT) } },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeModeOption(
                            label = "深色",
                            icon = Icons.Rounded.DarkMode,
                            selected = settings.themeMode == ThemeMode.DARK,
                            accent = TextPrimary,
                            onClick = { update { it.copy(themeMode = ThemeMode.DARK) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentCyan,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("刷新频率")
                        GlassPill(text = "每 ${formatOneDecimal(settings.refreshSeconds)} 秒", color = AccentCyan)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "仅在应用前台时通过 SSH 采样；切到后台会自动断开连接以节省电量。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(3.0, 5.0, 10.0, 15.0, 30.0, 60.0).forEach { seconds ->
                            val active = abs(settings.refreshSeconds - seconds) < 0.05
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) AccentCyan.copy(alpha = 0.16f) else GlassLine.copy(alpha = 0.045f))
                                    .border(
                                        width = 1.dp,
                                        color = if (active) AccentCyan.copy(alpha = 0.35f) else GlassLine.copy(alpha = 0.10f),
                                        shape = CircleShape,
                                    )
                                    .clickable { selectRefresh(seconds) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${formatRefreshSeconds(seconds)}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) AccentCyan else TextSecondary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = customRefresh,
                            onValueChange = { raw ->
                                val normalized = raw.replace(',', '.')
                                if (refreshInputPattern.matches(normalized)) {
                                    customRefresh = normalized
                                    customRefreshError = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text("自定义秒数", style = MaterialTheme.typography.labelMedium)
                            },
                            trailingIcon = {
                                Text(
                                    text = "秒",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(end = 10.dp),
                                )
                            },
                            singleLine = true,
                            isError = customRefreshError != null,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { applyCustomRefresh() }),
                            shape = RoundedCornerShape(15.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = AccentCyan,
                                focusedBorderColor = AccentCyan.copy(alpha = 0.75f),
                                unfocusedBorderColor = GlassLine.copy(alpha = 0.12f),
                                errorBorderColor = AccentRose.copy(alpha = 0.85f),
                                focusedContainerColor = GlassLine.copy(alpha = 0.045f),
                                unfocusedContainerColor = GlassLine.copy(alpha = 0.025f),
                                focusedLabelColor = AccentCyan,
                                unfocusedLabelColor = TextTertiary,
                                errorLabelColor = AccentRose,
                                focusedTrailingIconColor = AccentCyan,
                                unfocusedTrailingIconColor = TextTertiary,
                            ),
                        )
                        GlassActionButton(
                            text = "应用",
                            accent = AccentCyan,
                            primary = true,
                            onClick = { applyCustomRefresh() },
                        )
                    }
                    if (customRefreshError != null) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = customRefreshError.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentRose,
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "范围 1.0 – 300.0 秒，精确到 0.1 秒；采样脚本本身需要约 0.5 秒计算瞬时速率。",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentViolet,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    SectionLabel("终端字号")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        GlassIconButton(
                            icon = Icons.Rounded.Remove,
                            contentDescription = "减小字号",
                            onClick = {
                                update { it.copy(terminalFontSize = (it.terminalFontSize - 1).coerceAtLeast(9)) }
                            },
                            size = 38.dp,
                            iconSize = 18.dp,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${settings.terminalFontSize}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = TextPrimary,
                            )
                            Text(
                                text = "pt",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = "增大字号",
                            onClick = {
                                update { it.copy(terminalFontSize = (it.terminalFontSize + 1).coerceAtMost(22)) }
                            },
                            size = 38.dp,
                            iconSize = 18.dp,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Ink.copy(alpha = 0.65f))
                            .border(1.dp, GlassLine.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "root@nebula:~$ uptime\n 14:32:07 up 27 days,  load average: 0.62, 0.74, 0.69",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = settings.terminalFontSize.sp,
                                lineHeight = (settings.terminalFontSize * 1.4f).sp,
                            ),
                            color = TextSecondary,
                        )
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentEmerald,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    SectionLabel("安全")
                    Spacer(Modifier.height(12.dp))
                    SecurityRow(
                        icon = Icons.Rounded.Security,
                        title = "凭据本机加密",
                        description = "密码与私钥使用 Android Keystore 中的 AES-256-GCM 密钥加密，不上传任何服务器。",
                        color = AccentEmerald,
                    )
                    Spacer(Modifier.height(12.dp))
                    SecurityRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "主机指纹校验",
                        description = "首次连接记录服务器主机密钥，之后指纹变化会阻止连接，防止中间人攻击。",
                        color = AccentCyan,
                    )
                    Spacer(Modifier.height(12.dp))
                    SecurityRow(
                        icon = Icons.Rounded.WifiTethering,
                        title = "点对点 SSH",
                        description = "指标与终端都直接连接到你的服务器，不经过中间服务器或云端。",
                        color = AccentViolet,
                    )
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentAmber,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("演示数据")
                        if (demo != null) {
                            GlassPill(text = "已启用", color = AccentAmber)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (demo != null) {
                            "昵称为「${demo.name}」的演示服务器会生成模拟指标，不会发起任何网络连接。"
                        } else {
                            "添加一台模拟服务器，无需真实 SSH 即可预览卡片、展开态与详情页效果。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (demo != null) {
                        GlassActionButton(
                            text = "移除演示服务器",
                            accent = AccentAmber,
                            onClick = {
                                scope.launch {
                                    viewModel.deleteServer(demo.id)
                                        .onSuccess { viewModel.toast("已移除演示服务器") }
                                }
                            },
                        )
                    } else {
                        GlassActionButton(
                            text = "载入演示服务器",
                            icon = Icons.Rounded.Add,
                            accent = AccentAmber,
                            primary = true,
                            onClick = {
                                scope.launch {
                                    viewModel.addDemoServer()
                                        .onSuccess { viewModel.toast("演示服务器已载入") }
                                }
                            },
                        )
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentCyan,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    SectionLabel("开源致谢")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "站在这些优秀开源项目的肩膀上。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(12.dp))
                    CreditRow(
                        name = "Haze",
                        description = "Compose 毛玻璃与背景模糊",
                        license = "Apache-2.0",
                        stars = "2.6k",
                        url = "https://github.com/chrisbanes/haze",
                        accent = AccentCyan,
                        onOpen = { url -> openUrl(url) },
                    )
                    CreditRow(
                        name = "xterm.js",
                        description = "终端渲染引擎",
                        license = "MIT",
                        stars = "21.3k",
                        url = "https://github.com/xtermjs/xterm.js",
                        accent = AccentEmerald,
                        onOpen = { url -> openUrl(url) },
                    )
                    CreditRow(
                        name = "JSch (mwiede)",
                        description = "SSH2 协议与密钥支持",
                        license = "BSD",
                        stars = "1.1k",
                        url = "https://github.com/mwiede/jsch",
                        accent = AccentViolet,
                        onOpen = { url -> openUrl(url) },
                    )
                    CreditRow(
                        name = "Vico",
                        description = "Compose 实时折线图",
                        license = "Apache-2.0",
                        stars = "3.2k",
                        url = "https://github.com/patrykandpatrick/vico",
                        accent = AccentAmber,
                        onOpen = { url -> openUrl(url) },
                    )
                    CreditRow(
                        name = "Compose Shimmer",
                        description = "加载骨架微光",
                        license = "Apache-2.0",
                        stars = "1.1k",
                        url = "https://github.com/valentinilk/compose-shimmer",
                        accent = AccentCyan,
                        onOpen = { url -> openUrl(url) },
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        AccentCyan.copy(alpha = 0.3f),
                                        AccentViolet.copy(alpha = 0.2f),
                                    ),
                                ),
                            )
                            .border(1.dp, GlassLine.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "WatchCat · 1.0.1",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Text(
                        text = "为你的服务器做一面安静的玻璃窗",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.10f))
                .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CreditRow(
    name: String,
    description: String,
    license: String,
    stars: String,
    url: String,
    accent: Color,
    onOpen: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen(url) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = license,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = AccentAmber.copy(alpha = 0.85f),
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = stars,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            Spacer(Modifier.width(5.dp))
            Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

@Composable
private fun ThemeModeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = if (AppThemeState.isDark) 0.12f else 0.09f) else GlassLine.copy(alpha = 0.045f))
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.28f) else GlassLine.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else TextSecondary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextPrimary else TextSecondary,
        )
    }
}
