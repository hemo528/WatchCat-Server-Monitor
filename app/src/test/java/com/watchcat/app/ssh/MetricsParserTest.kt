package com.watchcat.app.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsParserTest {
    private val sample = """
        @@HOST
        test-node
        @@OS
        Ubuntu 24.04.1 LTS
        @@KERNEL
        Linux 6.8.0-45-generic
        @@ARCH
        x86_64
        @@UPTIME
        2345678
        @@CORES
        8
        @@LOAD
        0.42 0.55 0.61
        @@CPU
        27.4
        @@CORES_CPU
        12.0 35.0 48.0 22.0 61.0 18.0 44.0 9.0
        @@MEM
        16777216 7340032 2097152 1572864
        @@DISKS
        /dev/sda1|/|104857600|52428800|52428800
        /dev/sdb1|/data|209715200|94371840|115343360
        @@NET
        2469134 468134
        @@DISKIO
        4691356 2469134
        @@TEMP
        45123
        @@END
    """.trimIndent()

    @Test
    fun parsesFullSnapshot() {
        val metrics = MetricsParser.parse(sample)
        assertEquals("test-node", metrics.hostname)
        assertEquals("Ubuntu 24.04.1 LTS", metrics.os)
        assertEquals(8, metrics.cores)
        assertEquals(27.4f, metrics.cpuPercent, 0.01f)
        assertEquals(8, metrics.perCorePercent.size)
        assertEquals(16777216L, metrics.memTotalKb)
        assertEquals(9437184L, metrics.memUsedKb)
        assertEquals(56.25f, metrics.memUsedPercent, 0.1f)
        assertEquals(2, metrics.disks.size)
        assertEquals("/", metrics.disks.first().mount)
        assertEquals(50.0f, metrics.disks.first().usedPercent, 0.01f)
        assertEquals(4_938_268L, metrics.rxBytesPerSecond)
        assertEquals(9_382_712L, metrics.diskReadBytesPerSecond)
        assertEquals(45.123f, metrics.temperatureC ?: 0f, 0.01f)
    }

    @Test
    fun sortsRootDiskFirstThenBySize() {
        val metrics = MetricsParser.parse(sample)
        assertEquals("/", metrics.disks[0].mount)
        assertEquals("/data", metrics.disks[1].mount)
        assertEquals(50.0f, metrics.diskUsedPercent, 0.01f)
    }

    @Test
    fun missingEndMarkerIsRejected() {
        val broken = sample.replace("@@END", "")
        try {
            MetricsParser.parse(broken)
            throw AssertionError("Expected parse failure")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("不完整"))
        }
    }

    @Test
    fun temperatureWithoutValueIsNull() {
        val metrics = MetricsParser.parse(sample.replace("45123", ""))
        assertNull(metrics.temperatureC)
    }
}
