package com.watchcat.app.ui.components

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val oneDecimal = DecimalFormat("0.0")
private val oneDecimalLocale = DecimalFormat("0.0", java.text.DecimalFormatSymbols(Locale.US))
private val twoDecimals = DecimalFormat("0.00")

fun formatPercent(value: Float, decimals: Int = 0): String = when (decimals) {
    0 -> "${value.coerceIn(0f, 100f).toInt()}%"
    1 -> "${oneDecimal.format(value.coerceIn(0f, 100f))}%"
    else -> "${twoDecimals.format(value.coerceIn(0f, 100f))}%"
}

fun formatPercentValue(value: Float, decimals: Int = 0): String = when (decimals) {
    0 -> value.coerceIn(0f, 100f).toInt().toString()
    1 -> oneDecimal.format(value.coerceIn(0f, 100f))
    else -> twoDecimals.format(value.coerceIn(0f, 100f))
}

fun formatLoad(value: Float): String = if (value >= 100f) value.toInt().toString() else oneDecimal.format(value)

fun formatBytes(kb: Long): String {
    if (kb <= 0L) return "0 B"
    val bytes = kb * 1024.0
    return formatBytesValue(bytes)
}

fun formatBytesValue(bytes: Double): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var value = abs(bytes)
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    val formatted = if (value >= 100 || index == 0) "%.0f".format(Locale.US, value) else "%.1f".format(Locale.US, value)
    return "$formatted ${units[index]}"
}

fun formatRate(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0L) return "0 B/s"
    return formatBytesValue(bytesPerSecond.toDouble()) + "/s"
}

fun formatUptime(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val days = s / 86_400
    val hours = (s % 86_400) / 3_600
    val minutes = (s % 3_600) / 60
    return when {
        days > 0 -> "${days}天 ${hours}小时"
        hours > 0 -> "${hours}小时 ${minutes}分"
        minutes > 0 -> "${minutes}分"
        else -> "${s}秒"
    }
}

fun formatUptimeShort(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val days = s / 86_400
    val hours = (s % 86_400) / 3_600
    return when {
        days > 0 -> "${days}天"
        hours > 0 -> "${hours}小时"
        else -> "${(s % 3_600) / 60}分"
    }
}

fun formatRelativeTime(timestamp: Long?, now: Long = System.currentTimeMillis()): String {
    if (timestamp == null || timestamp <= 0L) return "尚未刷新"
    val seconds = ((now - timestamp) / 1000).coerceAtLeast(0)
    return when {
        seconds < 3 -> "刚刚"
        seconds < 60 -> "${seconds}秒前"
        seconds < 3_600 -> "${seconds / 60}分钟前"
        seconds < 86_400 -> "${seconds / 3_600}小时前"
        else -> "${seconds / 86_400}天前"
    }
}

fun formatDurationShort(ms: Long): String = when {
    ms < 1_000 -> "${ms}ms"
    ms < 60_000 -> "${oneDecimal.format(ms / 1000.0)}s"
    else -> "${oneDecimal.format(ms / 60_000.0)}m"
}

fun sanitizeSingleLine(value: String): String = value.replace('\n', ' ').trim()


fun formatOneDecimal(value: Double): String = oneDecimalLocale.format(value)

fun formatRefreshSeconds(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        oneDecimalLocale.format(rounded)
    }
}
