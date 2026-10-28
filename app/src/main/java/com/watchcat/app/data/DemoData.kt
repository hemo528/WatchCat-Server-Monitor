package com.watchcat.app.data

import kotlin.math.sin
import kotlin.random.Random

object DemoData {
    fun server(): ServerConfig = ServerConfig(
        id = "demo-nebula",
        name = "Nebula · 演示服务器",
        host = "demo.watchcat.app",
        port = 22,
        username = "demo",
        isDemo = true,
        accentIndex = 0,
        createdAt = System.currentTimeMillis(),
    )

    fun metrics(): Metrics {
        val t = System.currentTimeMillis() / 1000.0
        val jitter = Random.nextDouble(-2.2, 2.2)
        val cpu = (29.0 + 13.0 * sin(t / 11.0) + 6.0 * sin(t / 3.7) + jitter)
            .coerceIn(3.0, 94.0)
        val mem = (56.0 + 7.5 * sin(t / 27.0) + 1.8 * sin(t / 6.0)).coerceIn(30.0, 88.0)
        val cores = 8
        val perCore = List(cores) { index ->
            (cpu + 18.0 * sin(t / (3.4 + index) + index) + Random.nextDouble(-6.0, 6.0))
                .coerceIn(1.0, 99.0).toFloat()
        }
        val totalMemKb = 32L * 1024 * 1024
        val memAvailableKb = (totalMemKb * (1.0 - mem / 100.0)).toLong()
        val down = (1_450_000 + 900_000 * sin(t / 5.2) + Random.nextLong(-120_000, 180_000))
            .toLong().coerceAtLeast(28_000L)
        val up = (310_000 + 190_000 * sin(t / 7.4 + 1.2) + Random.nextLong(-40_000, 60_000))
            .toLong().coerceAtLeast(12_000L)
        val disks = listOf(
            DiskUsage("/dev/nvme0n1p2", "/", 512L * 1024 * 1024, 318L * 1024 * 1024, 194L * 1024 * 1024),
            DiskUsage("/dev/nvme1n1p1", "/data", 2L * 1024 * 1024 * 1024, 928L * 1024 * 1024, 1_120L * 1024 * 1024),
            DiskUsage("/dev/sda1", "/var/log", 128L * 1024 * 1024, 91L * 1024 * 1024, 37L * 1024 * 1024),
        )
        return Metrics(
            hostname = "nebula-01",
            os = "Ubuntu 24.04.2 LTS",
            kernel = "Linux 6.8.0-51-generic",
            arch = "x86_64",
            uptimeSeconds = 27 * 24 * 3600L + 4 * 3600L + 37 * 60L,
            cores = cores,
            load1 = (0.62f + (cpu.toFloat() / 100f) * 1.4f + Random.nextFloat() * 0.12f),
            load5 = 0.74f,
            load15 = 0.69f,
            cpuPercent = cpu.toFloat(),
            perCorePercent = perCore,
            memTotalKb = totalMemKb,
            memAvailableKb = memAvailableKb,
            swapTotalKb = 8L * 1024 * 1024,
            swapFreeKb = 6L * 1024 * 1024 + 512 * 1024,
            disks = disks,
            rxBytesPerSecond = down,
            txBytesPerSecond = up,
            diskReadBytesPerSecond = (42_000_000 + 20_000_000 * sin(t / 4.0)).toLong().coerceAtLeast(0L),
            diskWriteBytesPerSecond = (11_000_000 + 8_000_000 * sin(t / 6.0 + 2.0)).toLong().coerceAtLeast(0L),
            temperatureC = (46.5 + 3.0 * sin(t / 19.0)).toFloat(),
        )
    }
}
