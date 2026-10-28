package com.watchcat.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class AuthType {
    PASSWORD,
    PRIVATE_KEY,
}

@Serializable
data class ServerConfig(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String = "root",
    val authType: AuthType = AuthType.PASSWORD,
    val passwordEnc: String? = null,
    val privateKeyEnc: String? = null,
    val passphraseEnc: String? = null,
    val accentIndex: Int = 0,
    val fingerprint: String? = null,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayHost: String
        get() = if (port == 22) host else "$host:$port"
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class AppSettings(
    val refreshSeconds: Double = 5.0,
    val terminalFontSize: Int = 13,
    val showDemoBadge: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

data class Credentials(
    val password: String? = null,
    val privateKey: String? = null,
    val passphrase: String? = null,
)

data class DiskUsage(
    val filesystem: String,
    val mount: String,
    val totalKb: Long,
    val usedKb: Long,
    val availableKb: Long,
) {
    val usedPercent: Float
        get() = if (totalKb <= 0L) 0f else (usedKb.toDouble() / totalKb * 100.0).toFloat()
}

data class Metrics(
    val hostname: String,
    val os: String,
    val kernel: String,
    val arch: String,
    val uptimeSeconds: Long,
    val cores: Int,
    val load1: Float,
    val load5: Float,
    val load15: Float,
    val cpuPercent: Float,
    val perCorePercent: List<Float>,
    val memTotalKb: Long,
    val memAvailableKb: Long,
    val swapTotalKb: Long,
    val swapFreeKb: Long,
    val disks: List<DiskUsage>,
    val rxBytesPerSecond: Long,
    val txBytesPerSecond: Long,
    val diskReadBytesPerSecond: Long,
    val diskWriteBytesPerSecond: Long,
    val temperatureC: Float? = null,
    val sampledAt: Long = System.currentTimeMillis(),
) {
    val memUsedKb: Long get() = (memTotalKb - memAvailableKb).coerceAtLeast(0L)
    val memUsedPercent: Float
        get() = if (memTotalKb <= 0L) 0f else (memUsedKb.toDouble() / memTotalKb * 100.0).toFloat()

    val swapUsedKb: Long get() = (swapTotalKb - swapFreeKb).coerceAtLeast(0L)
    val swapUsedPercent: Float
        get() = if (swapTotalKb <= 0L) 0f else (swapUsedKb.toDouble() / swapTotalKb * 100.0).toFloat()

    val primaryDisk: DiskUsage?
        get() = disks.firstOrNull { it.mount == "/" } ?: disks.maxByOrNull { it.totalKb }

    val diskUsedPercent: Float
        get() = primaryDisk?.usedPercent ?: 0f

    val totalDiskUsedKb: Long get() = disks.sumOf { it.usedKb }
    val totalDiskTotalKb: Long get() = disks.sumOf { it.totalKb }
}

data class MetricSample(
    val timestamp: Long,
    val cpuPercent: Float,
    val memPercent: Float,
    val rxBytesPerSecond: Long,
    val txBytesPerSecond: Long,
)

enum class ServerStatus {
    IDLE,
    CONNECTING,
    ONLINE,
    OFFLINE,
}

data class ServerUiState(
    val status: ServerStatus = ServerStatus.IDLE,
    val metrics: Metrics? = null,
    val error: String? = null,
    val latencyMs: Long? = null,
    val lastUpdated: Long? = null,
    val history: List<MetricSample> = emptyList(),
    val isRefreshing: Boolean = false,
)
