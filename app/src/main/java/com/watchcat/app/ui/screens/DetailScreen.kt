package com.watchcat.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchcat.app.data.DiskUsage
import com.watchcat.app.data.Metrics
import com.watchcat.app.data.ServerConfig
import com.watchcat.app.data.ServerStatus
import com.watchcat.app.data.ServerUiState
import com.watchcat.app.ui.components.GlassActionButton
import com.watchcat.app.ui.components.GlassCard
import com.watchcat.app.ui.components.GlassDivider
import com.watchcat.app.ui.components.GlassIconButton
import com.watchcat.app.ui.components.GlassPill
import com.watchcat.app.ui.components.GlassTopBar
import com.watchcat.app.ui.components.KeyValueRow
import com.watchcat.app.ui.components.MetricBar
import com.watchcat.app.ui.components.MetricRing
import com.watchcat.app.ui.components.PerCoreBars
import com.watchcat.app.ui.components.SectionLabel
import com.watchcat.app.ui.components.ServerAvatar
import com.watchcat.app.ui.components.Sparkline
import com.watchcat.app.ui.components.StatusChip
import com.watchcat.app.ui.components.TrendChart
import com.watchcat.app.ui.components.formatBytes
import com.watchcat.app.ui.components.formatLoad
import com.watchcat.app.ui.components.formatPercent
import com.watchcat.app.ui.components.formatRate
import com.watchcat.app.ui.components.formatRelativeTime
import com.watchcat.app.ui.components.formatUptime
import com.watchcat.app.ui.components.formatUptimeShort
import com.watchcat.app.ui.components.glassSurface
import com.watchcat.app.ui.theme.AccentAmber
import com.watchcat.app.ui.theme.AccentBlue
import com.watchcat.app.ui.theme.AccentCyan
import com.watchcat.app.ui.theme.AccentEmerald
import com.watchcat.app.ui.theme.AccentRose
import com.watchcat.app.ui.theme.AccentViolet
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.InkSurfaceHigh
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.ui.theme.TextSecondary
import com.watchcat.app.ui.theme.TextTertiary
import com.watchcat.app.ui.theme.accentAt
import com.watchcat.app.ui.theme.usageColor
import com.watchcat.app.vm.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DetailScreen(
    viewModel: AppViewModel,
    serverId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTerminal: (String) -> Unit,
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val states by viewModel.states.collectAsStateWithLifecycle()
    val server = servers.firstOrNull { it.id == serverId }
    val state = states[serverId] ?: ServerUiState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var menuOpen by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showResetKey by remember { mutableStateOf(false) }

    if (server == null) {
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

    val accent = accentAt(server.accentIndex)

    fun copy(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        viewModel.toast("已复制$label")
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = server.name,
                subtitle = "${server.username}@${server.displayHost}",
                leading = {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                    )
                },
                actions = {
                    GlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "刷新",
                        onClick = { viewModel.refreshServer(server.id) },
                        tint = if (state.isRefreshing) AccentCyan else TextSecondary,
                    )
                    Box {
                        GlassIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = "更多",
                            onClick = { menuOpen = true },
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = InkSurfaceHigh,
                        ) {
                            if (!server.isDemo) {
                                DropdownMenuItem(
                                    text = { Text("编辑服务器", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = TextSecondary)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onEdit(server.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("重置主机指纹", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = TextSecondary)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        showResetKey = true
                                    },
                                    enabled = server.fingerprint != null,
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("删除服务器", color = AccentRose) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = AccentRose)
                                },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 150.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    HeroCard(
                        server = server,
                        state = state,
                        accent = accent,
                    )
                }

                if (state.metrics != null) {
                    item {
                        TrendCard(
                            state = state,
                            accent = accent,
                        )
                    }
                    item {
                        ResourceCard(
                            metrics = state.metrics,
                            accent = accent,
                        )
                    }
                    item {
                        DiskCard(
                            metrics = state.metrics,
                            accent = AccentBlue,
                        )
                    }
                    item {
                        NetworkCard(
                            metrics = state.metrics,
                            accent = AccentCyan,
                        )
                    }

                }

                item {
                    SystemCard(
                        server = server,
                        state = state,
                        accent = accent,
                        onCopyFingerprint = {
                            server.fingerprint?.let { copy("主机指纹", it) }
                        },
                    )
                }

                item {
                    Text(
                        text = "数据通过 SSH 直接从服务器读取，不经过任何第三方服务。",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }

        BottomActionBar(
            server = server,
            state = state,
            accent = accent,
            onTerminal = {
                if (server.isDemo) {
                    viewModel.toast("演示服务器不支持 SSH")
                } else {
                    onTerminal(server.id)
                }
            },
            onRefresh = { viewModel.refreshServer(server.id) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = InkSurfaceHigh,
            title = { Text("删除服务器？", color = TextPrimary) },
            text = {
                Text(
                    "将删除「${server.name}」的配置与本地加密凭据，不会影响服务器本身。",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        viewModel.deleteServer(server.id).onSuccess { onBack() }
                    }
                }) {
                    Text("删除", color = AccentRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
        )
    }

    if (showResetKey) {
        AlertDialog(
            onDismissRequest = { showResetKey = false },
            containerColor = InkSurfaceHigh,
            title = { Text("重置主机指纹？", color = TextPrimary) },
            text = {
                Text(
                    "下次连接会重新信任服务器当前的主机密钥。只有在确认服务器重装或密钥轮换时才应这样做，否则可能遭遇中间人攻击。",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetKey = false
                    scope.launch { viewModel.resetFingerprint(server.id) }
                }) {
                    Text("重置", color = AccentAmber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetKey = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun HeroCard(
    server: ServerConfig,
    state: ServerUiState,
    accent: Color,
) {
    val metrics = state.metrics
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServerAvatar(server = server, status = state.status, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = metrics?.hostname?.takeIf { it.isNotBlank() } ?: server.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (metrics != null) "${metrics.os} · ${metrics.arch}" else server.displayHost,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(
                status = state.status,
                latencyMs = state.latencyMs,
                isDemo = server.isDemo,
            )
        }

        if (metrics == null) {
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(
                    color = if (state.status == ServerStatus.OFFLINE) AccentRose else accent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when (state.status) {
                        ServerStatus.OFFLINE -> "暂时无法连接这台服务器"
                        ServerStatus.CONNECTING -> "正在建立 SSH 连接…"
                        else -> "等待第一次采样…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                if (!state.error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentRose,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricRing(
                    percent = metrics.cpuPercent,
                    accent = accent,
                    value = formatPercent(metrics.cpuPercent),
                    label = "CPU",
                    size = 86.dp,
                    strokeWidth = 7.dp,
                    valueSize = 18,
                )
                MetricRing(
                    percent = metrics.memUsedPercent,
                    accent = AccentViolet,
                    value = formatPercent(metrics.memUsedPercent),
                    label = "内存",
                    size = 86.dp,
                    strokeWidth = 7.dp,
                    valueSize = 18,
                )
                MetricRing(
                    percent = metrics.diskUsedPercent,
                    accent = AccentBlue,
                    value = formatPercent(metrics.diskUsedPercent),
                    label = "磁盘",
                    size = 86.dp,
                    strokeWidth = 7.dp,
                    valueSize = 18,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CPU 趋势",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    text = "更新于 ${formatRelativeTime(state.lastUpdated)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
            Spacer(Modifier.height(5.dp))
            Sparkline(
                values = state.history.map { it.cpuPercent },
                accent = accent,
                fixedRange = 0f..100f,
                showGrid = true,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroFact(
                    label = "负载",
                    value = "${formatLoad(metrics.load1)} / ${formatLoad(metrics.load5)} / ${formatLoad(metrics.load15)}",
                    modifier = Modifier.weight(1f),
                )
                HeroFact(
                    label = "运行时长",
                    value = formatUptimeShort(metrics.uptimeSeconds),
                    modifier = Modifier.weight(1f),
                )
                HeroFact(
                    label = "温度",
                    value = metrics.temperatureC?.let { "${it.roundToInt()}°C" } ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
            }

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentAmber.copy(alpha = 0.92f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun TrendCard(
    state: ServerUiState,
    accent: Color,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val history = state.history
    val tabs = listOf("CPU", "内存", "网络")
    val values: List<Float> = when (selected) {
        0 -> history.map { it.cpuPercent }
        1 -> history.map { it.memPercent }
        else -> history.map { it.rxBytesPerSecond.toFloat() }
    }
    val chartAccent = when (selected) {
        0 -> accent
        1 -> AccentViolet
        else -> AccentBlue
    }
    val current = when (selected) {
        0 -> state.metrics?.cpuPercent?.let { formatPercent(it, 1) } ?: "--"
        1 -> state.metrics?.memUsedPercent?.let { formatPercent(it, 1) } ?: "--"
        else -> state.metrics?.let { formatRate(it.rxBytesPerSecond) } ?: "--"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = chartAccent,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                SectionLabel("实时趋势")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = current,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "最近 ${history.size} 次采样",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(11.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(GlassLine.copy(alpha = 0.045f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            tabs.forEachIndexed { index, label ->
                val active = selected == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (active) chartAccent.copy(alpha = 0.18f) else Color.Transparent)
                        .then(
                            if (active) {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            chartAccent.copy(alpha = 0.16f),
                                            chartAccent.copy(alpha = 0.05f),
                                        ),
                                    ),
                                    CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { selected = index }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) chartAccent else TextTertiary,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        TrendChart(
            values = values,
            accent = chartAccent,
            fixedRange = if (selected == 0 || selected == 1) 0f..100f else null,
            chartHeight = 148.dp,
        )
    }
}

@Composable
private fun ResourceCard(metrics: Metrics, accent: Color) {
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
            SectionLabel("内存与计算")
            GlassPill(text = "${metrics.cores} 核心", color = TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        MetricBar(
            label = "内存",
            percent = metrics.memUsedPercent,
            valueText = "${formatBytes(metrics.memUsedKb)} / ${formatBytes(metrics.memTotalKb)}",
            accent = AccentViolet,
        )
        if (metrics.swapTotalKb > 0) {
            Spacer(Modifier.height(12.dp))
            MetricBar(
                label = "交换分区",
                percent = metrics.swapUsedPercent,
                valueText = "${formatBytes(metrics.swapUsedKb)} / ${formatBytes(metrics.swapTotalKb)}",
                accent = AccentBlue,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "每核心负载",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = "1m ${formatLoad(metrics.load1)} · 5m ${formatLoad(metrics.load5)} · 15m ${formatLoad(metrics.load15)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(8.dp))
        PerCoreBars(values = metrics.perCorePercent, accent = accent)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InlineFact(
                icon = Icons.Rounded.Speed,
                label = "负载",
                value = formatLoad(metrics.load1),
                modifier = Modifier.weight(1f),
            )
            InlineFact(
                icon = Icons.Rounded.Thermostat,
                label = "温度",
                value = metrics.temperatureC?.let { "${it.roundToInt()}°C" } ?: "N/A",
                modifier = Modifier.weight(1f),
            )
            InlineFact(
                icon = Icons.Rounded.Schedule,
                label = "运行",
                value = formatUptimeShort(metrics.uptimeSeconds),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InlineFact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun DiskCard(metrics: Metrics, accent: Color) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("磁盘")
            GlassPill(
                text = "${metrics.disks.size} 个挂载点",
                color = TextSecondary,
                icon = Icons.Rounded.Storage,
            )
        }
        Spacer(Modifier.height(14.dp))
        if (metrics.disks.isEmpty()) {
            Text("服务器未返回可用的磁盘信息", color = TextTertiary)
        } else {
            metrics.disks.forEachIndexed { index, disk ->
                if (index > 0) Spacer(Modifier.height(13.dp))
                DiskDetailRow(disk = disk, accent = accent)
            }
        }
    }
}

@Composable
private fun DiskDetailRow(disk: DiskUsage, accent: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = disk.mount,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Text(
                    text = disk.filesystem,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatPercent(disk.usedPercent),
                style = MaterialTheme.typography.titleSmall,
                color = usageColor(disk.usedPercent, accent),
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(GlassLine.copy(alpha = 0.07f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((disk.usedPercent / 100f).coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                usageColor(disk.usedPercent, accent).copy(alpha = 0.7f),
                                usageColor(disk.usedPercent, accent),
                            ),
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = "${formatBytes(disk.usedKb)} 已用 · ${formatBytes(disk.availableKb)} 可用 · 共 ${formatBytes(disk.totalKb)}",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun NetworkCard(metrics: Metrics, accent: Color) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        contentPadding = PaddingValues(18.dp),
    ) {
        SectionLabel("网络与磁盘 I/O")
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NetworkStat(
                label = "下行",
                value = formatRate(metrics.rxBytesPerSecond),
                color = AccentCyan,
                modifier = Modifier.weight(1f),
            )
            NetworkStat(
                label = "上行",
                value = formatRate(metrics.txBytesPerSecond),
                color = AccentViolet,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NetworkStat(
                label = "磁盘读取",
                value = formatRate(metrics.diskReadBytesPerSecond),
                color = AccentBlue,
                modifier = Modifier.weight(1f),
            )
            NetworkStat(
                label = "磁盘写入",
                value = formatRate(metrics.diskWriteBytesPerSecond),
                color = AccentEmerald,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NetworkStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.075f))
            .padding(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SystemCard(
    server: ServerConfig,
    state: ServerUiState,
    accent: Color,
    onCopyFingerprint: () -> Unit,
) {
    val metrics = state.metrics
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("系统信息")
            GlassPill(text = "SSH ${server.displayHost}", color = TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        if (metrics != null) {
            KeyValueRow(label = "主机名", value = metrics.hostname)
            Spacer(Modifier.height(9.dp))
            KeyValueRow(label = "操作系统", value = metrics.os)
            Spacer(Modifier.height(9.dp))
            KeyValueRow(label = "内核", value = metrics.kernel)
            Spacer(Modifier.height(9.dp))
            KeyValueRow(label = "架构", value = metrics.arch)
            Spacer(Modifier.height(9.dp))
            KeyValueRow(label = "运行时长", value = formatUptime(metrics.uptimeSeconds))
            Spacer(Modifier.height(9.dp))
            KeyValueRow(label = "登录用户", value = server.username)
            Spacer(Modifier.height(9.dp))
            KeyValueRow(label = "最后更新", value = formatRelativeTime(state.lastUpdated))
        } else {
            KeyValueRow(label = "地址", value = "${server.host}:${server.port}")
            Spacer(Modifier.height(9.dp))
            KeyValueRow(label = "登录用户", value = server.username)
        }
        Spacer(Modifier.height(13.dp))
        GlassDivider()
        Spacer(Modifier.height(13.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = server.fingerprint != null, onClick = onCopyFingerprint)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "主机指纹",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                Text(
                    text = server.fingerprint ?: "首次连接成功后自动记录",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (server.fingerprint != null) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = "复制指纹",
                    tint = TextTertiary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    server: ServerConfig,
    state: ServerUiState,
    accent: Color,
    onTerminal: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(
                shape = RoundedCornerShape(26.dp),
                accent = accent,
                fillAlpha = 0.075f,
                borderAlpha = 0.24f,
                elevated = true,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        GlassActionButton(
            text = if (server.isDemo) "演示模式" else "打开 SSH 终端",
            icon = Icons.Rounded.Terminal,
            accent = accent,
            primary = true,
            enabled = !server.isDemo,
            onClick = onTerminal,
            modifier = Modifier.weight(1f),
        )
        GlassIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "立即刷新",
            onClick = onRefresh,
            tint = if (state.isRefreshing) AccentCyan else TextSecondary,
            size = 46.dp,
            iconSize = 20.dp,
        )
    }
}

