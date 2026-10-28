package com.watchcat.app.ssh

import com.watchcat.app.data.DiskUsage
import com.watchcat.app.data.Metrics
import java.util.Locale
import kotlin.math.roundToLong

object MetricsParser {
    private const val SAMPLE_SECONDS = 0.5

    fun parse(raw: String): Metrics {
        val sections = parseSections(raw)
        if (sections.isEmpty()) throw IllegalStateException("服务器没有返回任何指标数据")
        if (!sections.containsKey("END")) {
            throw IllegalStateException("指标输出不完整，请检查服务器的 shell 环境")
        }

        fun section(name: String): List<String> = sections[name].orEmpty()

        fun text(name: String, fallback: String = ""): String =
            section(name).firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

        fun long(name: String, fallback: Long = 0L): Long =
            text(name).toDoubleOrNull()?.toLong() ?: fallback

        fun float(name: String, fallback: Float = 0f): Float =
            text(name).toDoubleOrNull()?.toFloat() ?: fallback

        val perCore = section("CORES_CPU")
            .flatMap { it.trim().split(Regex("\\s+")) }
            .mapNotNull { it.toFloatOrNull() }
            .map { it.coerceIn(0f, 100f) }

        val cpuRaw = float("CPU", -1f)
        val cpu = when {
            cpuRaw >= 0f -> cpuRaw
            perCore.isNotEmpty() -> perCore.average().toFloat()
            else -> 0f
        }.coerceIn(0f, 100f)

        val load = text("LOAD", "0 0 0").split(Regex("\\s+")).mapNotNull { it.toFloatOrNull() }
        val mem = text("MEM").split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
        val memTotal = mem.getOrElse(0) { 0L }
        val memAvailable = mem.getOrElse(1) { 0L }
        val swapTotal = mem.getOrElse(2) { 0L }
        val swapFree = mem.getOrElse(3) { 0L }

        val disks = section("DISKS").mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 5) return@mapNotNull null
            val total = parts[2].toLongOrNull() ?: return@mapNotNull null
            val used = parts[3].toLongOrNull() ?: return@mapNotNull null
            val available = parts[4].toLongOrNull() ?: return@mapNotNull null
            if (total <= 0L) return@mapNotNull null
            DiskUsage(
                filesystem = parts[0].trim(),
                mount = parts[1].trim(),
                totalKb = total,
                usedKb = used.coerceIn(0L, total),
                availableKb = available.coerceAtLeast(0L),
            )
        }
            .distinctBy { it.mount }
            .sortedWith(compareByDescending<DiskUsage> { it.mount == "/" }.thenByDescending { it.totalKb })
            .take(10)

        val net = parseRates(text("NET"))
        val io = parseRates(text("DISKIO"))

        val tempRaw = text("TEMP").toFloatOrNull()
        val temperature = tempRaw?.let {
            val value = if (it > 1000f) it / 1000f else it
            value.takeIf { v -> v in 1f..150f }
        }

        val os = text("OS", "Linux").trim('"', '\'')
        val hostname = text("HOST", "-")
        val cores = maxOf(long("CORES", 1L).toInt(), perCore.size, 1)

        return Metrics(
            hostname = hostname,
            os = os,
            kernel = text("KERNEL", "-"),
            arch = text("ARCH", "-"),
            uptimeSeconds = long("UPTIME", 0L).coerceAtLeast(0L),
            cores = cores.coerceIn(1, 512),
            load1 = load.getOrElse(0) { 0f },
            load5 = load.getOrElse(1) { 0f },
            load15 = load.getOrElse(2) { 0f },
            cpuPercent = cpu,
            perCorePercent = perCore,
            memTotalKb = memTotal.coerceAtLeast(0L),
            memAvailableKb = memAvailable.coerceIn(0L, memTotal.coerceAtLeast(0L)),
            swapTotalKb = swapTotal.coerceAtLeast(0L),
            swapFreeKb = swapFree.coerceIn(0L, swapTotal.coerceAtLeast(0L)),
            disks = disks,
            rxBytesPerSecond = net.first,
            txBytesPerSecond = net.second,
            diskReadBytesPerSecond = io.first,
            diskWriteBytesPerSecond = io.second,
            temperatureC = temperature,
        )
    }

    private fun parseRates(value: String): Pair<Long, Long> {
        val parts = value.trim().split(Regex("\\s+"))
        val rx = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        val tx = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        return Pair(
            (rx / SAMPLE_SECONDS).roundToLong().coerceAtLeast(0L),
            (tx / SAMPLE_SECONDS).roundToLong().coerceAtLeast(0L),
        )
    }

    private fun parseSections(raw: String): Map<String, List<String>> {
        val result = LinkedHashMap<String, MutableList<String>>()
        var current: String? = null
        raw.lineSequence().forEach { line ->
            val trimmed = line.trimEnd('\r')
            if (trimmed.startsWith("@@")) {
                current = trimmed.removePrefix("@@").trim().uppercase(Locale.US)
                result.getOrPut(current) { mutableListOf() }
            } else if (current != null) {
                result[current]?.add(trimmed)
            }
        }
        return result
    }
}
