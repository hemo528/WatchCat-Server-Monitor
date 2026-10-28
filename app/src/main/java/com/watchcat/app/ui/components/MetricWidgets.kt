package com.watchcat.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchcat.app.data.ServerConfig
import com.watchcat.app.data.ServerStatus
import com.watchcat.app.ui.theme.AccentAmber
import com.watchcat.app.ui.theme.AccentCyan
import com.watchcat.app.ui.theme.AccentEmerald
import com.watchcat.app.ui.theme.AccentRose
import com.watchcat.app.ui.theme.GlassLine
import com.watchcat.app.ui.theme.Ink
import com.watchcat.app.ui.theme.OnAccent
import com.watchcat.app.ui.theme.NumericBoldStyle
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.ui.theme.TextSecondary
import com.watchcat.app.ui.theme.TextTertiary
import com.watchcat.app.ui.theme.accentAt
import com.watchcat.app.ui.theme.usageColor

@Composable
fun MetricBar(
    label: String,
    percent: Float,
    valueText: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "metricBar",
    )
    val color = usageColor(percent, accent)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 1.sp,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = NumericBoldStyle.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (percent >= 90f) AccentRose else TextPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(GlassLine.copy(alpha = 0.075f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                color.copy(alpha = 0.65f),
                                color,
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
fun MetricRing(
    percent: Float,
    accent: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    strokeWidth: Dp = 8.dp,
    valueSize: Int = 20,
) {
    val animated by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "metricRing",
    )
    val color = usageColor(percent, accent)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f + 1.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(
                width = this.size.width - inset * 2,
                height = this.size.height - inset * 2,
            )
            drawArc(
                color = GlassLine.copy(alpha = 0.07f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.003f) {
                drawArc(
                    color = color.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke + 4.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = NumericBoldStyle.copy(fontSize = valueSize.sp, lineHeight = (valueSize + 2).sp),
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun Sparkline(
    values: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    fixedRange: ClosedFloatingPointRange<Float>? = null,
    showGrid: Boolean = false,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (showGrid) {
            for (i in 1..3) {
                val y = h * i / 4f
                drawLine(
                    color = GlassLine.copy(alpha = 0.035f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        val source = when {
            values.isEmpty() -> listOf(0f, 0f)
            values.size == 1 -> listOf(values[0], values[0])
            else -> values
        }
        val minValue: Float
        val valMax: Float
        if (fixedRange != null) {
            minValue = fixedRange.start
            valMax = fixedRange.endInclusive
        } else {
            val rawMin = source.min()
            val rawMax = source.max()
            val padding = (rawMax - rawMin).coerceAtLeast(1f) * 0.18f
            minValue = rawMin - padding
            valMax = rawMax + padding
        }
        val range = (valMax - minValue).coerceAtLeast(0.001f)
        val horizontalPadding = strokeWidth.toPx() * 1.5f
        val topPadding = 4.dp.toPx()
        val bottomPadding = 4.dp.toPx()
        val usableW = w - horizontalPadding * 2
        val usableH = h - topPadding - bottomPadding

        val points = source.mapIndexed { index, value ->
            val x = horizontalPadding + usableW * (index.toFloat() / (source.size - 1).coerceAtLeast(1))
            val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
            val y = topPadding + usableH * (1f - normalized)
            androidx.compose.ui.geometry.Offset(x, y)
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val current = points[i]
                val midX = (prev.x + current.x) / 2f
                cubicTo(midX, prev.y, midX, current.y, current.x, current.y)
            }
        }

        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.30f),
                    accent.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                startY = topPadding,
                endY = h,
            ),
        )
        drawPath(
            path = line,
            color = accent.copy(alpha = 0.20f),
            style = Stroke(width = strokeWidth.toPx() * 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = line,
            brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f), accent)),
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val last = points.last()
        drawCircle(color = accent.copy(alpha = 0.18f), radius = 5.dp.toPx(), center = last)
        drawCircle(color = accent, radius = 2.dp.toPx(), center = last)
    }
}

@Composable
fun PerCoreBars(
    values: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    maxBars: Int = 32,
) {
    val bars = values.take(maxBars)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { value ->
                val fraction by animateFloatAsState(
                    targetValue = (value / 100f).coerceIn(0.03f, 1f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                    label = "coreBar",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(GlassLine.copy(alpha = 0.055f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(2.dp))
                            .background(usageColor(value, accent)),
                    )
                }
            }
        }
        if (values.size > maxBars) {
            Text(
                text = "+${values.size - maxBars} 核心",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun StatusDot(
    status: ServerStatus,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    val color = when (status) {
        ServerStatus.ONLINE -> AccentEmerald
        ServerStatus.CONNECTING -> AccentCyan
        ServerStatus.OFFLINE -> AccentRose
        ServerStatus.IDLE -> TextTertiary
    }
    val transition = rememberInfiniteTransition(label = "statusPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusPulseAlpha",
    )
    Canvas(modifier.size(size * 2.8f)) {
        val center = androidx.compose.ui.geometry.Offset(this.size.width / 2, this.size.height / 2)
        val radius = this.size.minDimension / 2
        val alpha = when (status) {
            ServerStatus.ONLINE -> 0.55f
            ServerStatus.CONNECTING -> pulse
            ServerStatus.OFFLINE -> 0.30f
            ServerStatus.IDLE -> 0.18f
        }
        drawCircle(color = color.copy(alpha = 0.16f * alpha), radius = radius * 1.9f, center = center)
        drawCircle(color = color.copy(alpha = 0.22f * alpha), radius = radius * 1.35f, center = center)
        drawCircle(color = color.copy(alpha = 0.95f), radius = radius * 0.55f, center = center)
    }
}

@Composable
fun StatusChip(
    status: ServerStatus,
    latencyMs: Long? = null,
    isDemo: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when (status) {
        ServerStatus.ONLINE -> (if (isDemo) "演示 · 在线" else "在线") to AccentEmerald
        ServerStatus.CONNECTING -> "连接中" to AccentCyan
        ServerStatus.OFFLINE -> "离线" to AccentRose
        ServerStatus.IDLE -> "等待" to TextTertiary
    }
    val suffix = if (status == ServerStatus.ONLINE && latencyMs != null && !isDemo) " · ${latencyMs}ms" else ""
    GlassPill(
        text = text + suffix,
        modifier = modifier,
        color = color,
        background = color.copy(alpha = 0.12f),
        borderColor = color.copy(alpha = 0.24f),
        leading = { StatusDot(status, size = 5.dp) },
    )
}

@Composable
fun ServerAvatar(
    server: ServerConfig,
    status: ServerStatus,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val accent = accentAt(server.accentIndex)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * 0.34f))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.85f),
                            accent.copy(alpha = 0.35f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(size * 0.34f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = server.name.trim().take(1).uppercase().ifEmpty { "S" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                ),
                color = OnAccent,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.34f)
                .clip(CircleShape)
                .background(Ink.copy(alpha = 0.92f))
                .border(1.dp, GlassLine.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            StatusDot(status, size = size * 0.11f)
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = NumericBoldStyle.fontFamily),
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
fun WarnDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}
