package com.watchcat.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.watchcat.app.ui.components.GlassTopBar
import com.watchcat.app.ui.components.MetricBar
import com.watchcat.app.ui.components.ServerAvatar
import com.watchcat.app.ui.components.Sparkline
import com.watchcat.app.ui.components.StatusChip
import com.watchcat.app.ui.components.StatusDot
import com.watchcat.app.ui.components.formatBytes
import com.watchcat.app.ui.components.formatLoad
import com.watchcat.app.ui.components.formatPercent
import com.watchcat.app.ui.components.formatRate
import com.watchcat.app.ui.components.formatOneDecimal
import com.watchcat.app.ui.components.formatRelativeTime
import com.watchcat.app.ui.components.formatUptimeShort
import com.watchcat.app.ui.theme.AccentAmber
import com.watchcat.app.ui.theme.AccentBlue
import com.watchcat.app.ui.theme.AccentCyan
import com.watchcat.app.ui.theme.AccentEmerald
import com.watchcat.app.ui.theme.AccentRose
import com.watchcat.app.ui.theme.AccentViolet
import com.watchcat.app.ui.theme.AppThemeState
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.ui.theme.TextSecondary
import com.watchcat.app.ui.theme.TextTertiary
import com.watchcat.app.ui.theme.accentAt
import com.watchcat.app.ui.theme.usageColor
import com.watchcat.app.vm.AppViewModel
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onAddServer: () -> Unit,
    onOpenServer: (String) -> Unit,
    onOpenTerminal: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val states by viewModel.states.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val onlineCount = servers.count { states[it.id]?.status == ServerStatus.ONLINE }
    val subtitle = if (!loaded) {
        "正在读取服务器…"
    } else if (servers.isEmpty()) {
        "还没有服务器"
    } else {
        "$onlineCount/${servers.size} 台在线 · 每 ${formatOneDecimal(settings.refreshSeconds)} 秒刷新"
    }

    Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "WatchCat",
                subtitle = subtitle,
                actions = {
                    GlassIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "添加服务器",
                        onClick = onAddServer,
                        tint = AccentCyan,
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "刷新全部",
                        onClick = { viewModel.refreshAll() },
                        tint = if (servers.any { states[it.id]?.isRefreshing == true }) AccentCyan else TextSecondary,
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "设置",
                        onClick = onOpenSettings,
                    )
                },
            )

            val shimmer = rememberShimmer(com.valentinilk.shimmer.ShimmerBounds.Window)

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!loaded) {
                    items(3) { SkeletonServerCard(shimmer) }
                    return@LazyColumn
                }

                if (servers.isEmpty()) {
                    item {
                        EmptyState(
                            onAddServer = onAddServer,
                            onAddDemo = {
                                scope.launch {
                                    viewModel.addDemoServer()
                                        .onSuccess { viewModel.toast("演示服务器已载入") }
                                        .onFailure { viewModel.toast("载入失败：${it.message ?: "未知错误"}") }
                                }
                            },
                        )
                    }
                    return@LazyColumn
                }

                item {
                    OverviewCard(
                        servers = servers,
                        states = states,
                        onlineCount = onlineCount,
                    )
                }

                items(servers, key = { it.id }) { server ->
                    val state = states[server.id] ?: ServerUiState()
                    ServerCard(
                        server = server,
                        state = state,
                        onToggleDetail = { onOpenServer(server.id) },
                        onTerminal = {
                            if (server.isDemo) {
                                viewModel.toast("演示服务器不支持 SSH，添加真实服务器后即可使用")
                            } else {
                                onOpenTerminal(server.id)
                            }
                        },
                        onRefresh = { viewModel.refreshServer(server.id) },
                    )
                }

                item {
                    AddServerRow(onClick = onAddServer)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "SSH 直连 · 凭据仅保存在本机",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                        )
                    }
                }
            }
    }
}

@Composable
private fun OverviewCard(
    servers: List<ServerConfig>,
    states: Map<String, ServerUiState>,
    onlineCount: Int,
) {
    val metrics = servers.mapNotNull { states[it.id]?.metrics }
    val avgCpu = if (metrics.isEmpty()) 0f else metrics.map { it.cpuPercent }.average().toFloat()
    val alerts = metrics.count {
        it.cpuPercent >= 90f || it.memUsedPercent >= 90f || it.disks.any { disk -> disk.usedPercent >= 90f }
    }
    val accent = AccentCyan

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(ServerStatus.ONLINE, size = 5.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "实时概览",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp,
                )
            }
            Text(
                text = "自动刷新中",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OverviewMetric(
                value = "$onlineCount/${servers.size}",
                label = "在线节点",
                color = AccentEmerald,
                modifier = Modifier.weight(1f),
            )
            OverviewMetric(
                value = formatPercent(avgCpu),
                label = "平均 CPU",
                color = usageColor(avgCpu, AccentCyan),
                modifier = Modifier.weight(1f),
            )
            OverviewMetric(
                value = if (alerts == 0) "正常" else "$alerts 项",
                label = "资源告警",
                color = if (alerts == 0) AccentEmerald else AccentAmber,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverviewMetric(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerConfig,
    state: ServerUiState,
    onToggleDetail: () -> Unit,
    onTerminal: () -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by rememberSaveable(server.id) { mutableStateOf(false) }
    val accent = accentAt(server.accentIndex)
    val metrics = state.metrics
    val offline = state.status == ServerStatus.OFFLINE
    val dim = if (offline) 0.55f else 1f

    GlassCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        accent = accent,
        contentPadding = PaddingValues(0.dp),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ServerAvatar(server = server, status = state.status)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary.copy(alpha = dim),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${server.username}@${server.displayHost}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    StatusChip(
                        status = state.status,
                        latencyMs = state.latencyMs,
                        isDemo = server.isDemo,
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = TextTertiary,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(19.dp)
                            .rotate(if (expanded) 180f else 0f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().graphicsAlpha(dim),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MetricBar(
                    label = "CPU",
                    percent = metrics?.cpuPercent ?: 0f,
                    valueText = metrics?.let { formatPercent(it.cpuPercent) } ?: "--",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                MetricBar(
                    label = "内存",
                    percent = metrics?.memUsedPercent ?: 0f,
                    valueText = metrics?.let { formatPercent(it.memUsedPercent) } ?: "--",
                    accent = AccentViolet,
                    modifier = Modifier.weight(1f),
                )
                MetricBar(
                    label = "磁盘",
                    percent = metrics?.diskUsedPercent ?: 0f,
                    valueText = metrics?.let { formatPercent(it.diskUsedPercent) } ?: "--",
                    accent = AccentBlue,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(13.dp))

            Row(
                modifier = Modifier.fillMaxWidth().graphicsAlpha(dim),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FooterStat(
                    icon = Icons.Rounded.Speed,
                    text = metrics?.let { "负载 ${formatLoad(it.load1)}" } ?: "负载 --",
                    modifier = Modifier.weight(1f),
                )
                FooterStat(
                    icon = Icons.Rounded.CloudDownload,
                    text = metrics?.let { formatRate(it.rxBytesPerSecond) } ?: "下行 --",
                    modifier = Modifier.weight(1f),
                )
                FooterStat(
                    icon = Icons.Rounded.CloudUpload,
                    text = metrics?.let { formatRate(it.txBytesPerSecond) } ?: "上行 --",
                    modifier = Modifier.weight(1f),
                )
            }

            if (offline && !state.error.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AccentRose,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentRose.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(260)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(140)),
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    GlassDivider()
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "CPU 趋势",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        Text(
                            text = formatRelativeTime(state.lastUpdated),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Sparkline(
                        values = state.history.map { it.cpuPercent },
                        accent = accent,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        fixedRange = 0f..100f,
                        showGrid = true,
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExpandedFact(
                            label = "内存",
                            value = metrics?.let { "${formatBytes(it.memUsedKb)} / ${formatBytes(it.memTotalKb)}" } ?: "--",
                            modifier = Modifier.weight(1f),
                        )
                        ExpandedFact(
                            label = "交换分区",
                            value = metrics?.let {
                                if (it.swapTotalKb <= 0) "未启用" else "${formatBytes(it.swapUsedKb)} / ${formatBytes(it.swapTotalKb)}"
                            } ?: "--",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExpandedFact(
                            label = "运行时长",
                            value = metrics?.let { formatUptimeShort(it.uptimeSeconds) } ?: "--",
                            modifier = Modifier.weight(1f),
                        )
                        ExpandedFact(
                            label = "温度 / 核心",
                            value = metrics?.let {
                                val temp = it.temperatureC?.let { t -> "${t.roundToInt()}°C" } ?: "N/A"
                                "$temp · ${it.cores}C"
                            } ?: "--",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    val disks = metrics?.disks.orEmpty().take(2)
                    if (disks.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        disks.forEach { disk ->
                            DiskRow(disk = disk, accent = AccentBlue)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassActionButton(
                            text = "完整详情",
                            icon = Icons.Rounded.OpenInFull,
                            accent = accent,
                            primary = true,
                            onClick = onToggleDetail,
                            modifier = Modifier.weight(1f),
                        )
                        GlassActionButton(
                            text = "SSH 终端",
                            icon = Icons.Rounded.Terminal,
                            accent = AccentEmerald,
                            onClick = onTerminal,
                            modifier = Modifier.weight(1f),
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = "刷新",
                            onClick = onRefresh,
                            tint = if (state.isRefreshing) AccentCyan else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun DiskRow(disk: DiskUsage, accent: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = disk.mount,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Text(
                text = "${formatBytes(disk.usedKb)} / ${formatBytes(disk.totalKb)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(GlassLine.copy(alpha = 0.07f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((disk.usedPercent / 100f).coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(usageColor(disk.usedPercent, accent)),
            )
        }
    }
}

@Composable
private fun FooterStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddServerRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, GlassLine.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .background(GlassLine.copy(alpha = 0.025f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AccentCyan.copy(alpha = 0.13f))
                .border(1.dp, AccentCyan.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "添加服务器",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = "通过 SSH 采集 CPU、内存与磁盘状态",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        Text(
            text = "SSH",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun EmptyState(
    onAddServer: () -> Unit,
    onAddDemo: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = AccentCyan,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 30.dp),
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            AccentCyan.copy(alpha = 0.22f),
                            AccentViolet.copy(alpha = 0.14f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Dns,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "把服务器放进玻璃卡片里",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "添加一台 Linux 服务器，WatchCat 会通过 SSH 定时采集 CPU、内存、磁盘与网络状态。所有凭据仅保存在本机 Keystore 加密存储中。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(22.dp))
        GlassActionButton(
            text = "添加第一台服务器",
            icon = Icons.Rounded.Add,
            accent = AccentCyan,
            primary = true,
            onClick = onAddServer,
        )
        Spacer(Modifier.height(10.dp))
        GlassActionButton(
            text = "载入演示服务器",
            accent = AccentViolet,
            onClick = onAddDemo,
        )
    }
}

@Composable
private fun SkeletonServerCard(shimmer: Shimmer) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = Color.White,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassLine.copy(alpha = 0.07f))
                    .shimmer(shimmer),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.45f)
                        .height(15.dp)
                        .clip(CircleShape)
                        .background(GlassLine.copy(alpha = 0.08f))
                        .shimmer(shimmer),
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.65f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(GlassLine.copy(alpha = 0.055f))
                        .shimmer(shimmer),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) {
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.55f)
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(GlassLine.copy(alpha = 0.06f))
                            .shimmer(shimmer),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(GlassLine.copy(alpha = 0.07f))
                            .shimmer(shimmer),
                    )
                }
            }
        }
    }
}

private fun Modifier.graphicsAlpha(alpha: Float): Modifier =
    this.graphicsLayer { this.alpha = alpha }
