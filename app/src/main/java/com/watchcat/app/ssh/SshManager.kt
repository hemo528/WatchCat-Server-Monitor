package com.watchcat.app.ssh

import android.content.Context
import android.util.Base64
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.watchcat.app.data.Credentials
import com.watchcat.app.data.Metrics
import com.watchcat.app.data.ServerConfig
import com.watchcat.app.data.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class MetricsResult(
    val metrics: Metrics,
    val latencyMs: Long,
)

class ConnectionTestResult(
    val latencyMs: Long,
    val fingerprint: String,
    val hostKeyType: String,
)

class HostKeyChangedException(
    val expected: String?,
    val actual: String?,
) : Exception("主机指纹已变化")

class ShellHandle(
    private val channel: ChannelShell,
    val input: InputStream,
    val output: OutputStream,
) {
    fun resize(cols: Int, rows: Int) {
        if (cols <= 0 || rows <= 0) return
        runCatching { channel.setPtySize(cols, rows, cols * 8, rows * 17) }
    }

    fun close() {
        runCatching { output.close() }
        runCatching { input.close() }
        runCatching { channel.disconnect() }
    }
}

class SshManager(context: Context, private val repository: ServerRepository) {
    private val appContext = context.applicationContext
    private val sessions = ConcurrentHashMap<String, Session>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    private val metricsScript: String by lazy {
        appContext.assets.open("metrics.sh").bufferedReader().use { it.readText() }
    }
    private val metricsCommand: String by lazy {
        "exec /bin/sh -c " + shellQuote(metricsScript)
    }

    suspend fun fetchMetrics(server: ServerConfig): MetricsResult = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        try {
            val session = sessionFor(server)
            val raw = runExec(session, metricsCommand, timeoutMs = 18_000)
            val metrics = MetricsParser.parse(raw)
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            MetricsResult(
                metrics = metrics.copy(sampledAt = System.currentTimeMillis()),
                latencyMs = (elapsedMs - 500L).coerceIn(1L, 60_000L),
            )
        } catch (t: Throwable) {
            invalidate(server.id)
            throw t
        }
    }

    suspend fun testConnection(
        server: ServerConfig,
        credentials: Credentials,
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        val connected = connectSession(
            server = server,
            credentials = credentials,
            persistFingerprint = false,
        )
        try {
            runExec(connected.session, "printf 'watchcat-ok\\n'", timeoutMs = 10_000)
            ConnectionTestResult(
                latencyMs = ((System.nanoTime() - startedAt) / 1_000_000).coerceAtLeast(1L),
                fingerprint = connected.fingerprint ?: "-",
                hostKeyType = connected.hostKeyType ?: "unknown",
            )
        } finally {
            runCatching { connected.session.disconnect() }
        }
    }

    suspend fun openShell(
        server: ServerConfig,
        cols: Int,
        rows: Int,
    ): ShellHandle = withContext(Dispatchers.IO) {
        val session = sessionFor(server)
        var channel: ChannelShell? = null
        try {
            channel = session.openChannel("shell") as ChannelShell
            channel.setPtyType("xterm-256color", cols.coerceAtLeast(20), rows.coerceAtLeast(5), cols * 8, rows * 17)
            channel.setInputStream(null)
            val input = channel.inputStream
            val output = channel.outputStream
            channel.connect(SHELL_TIMEOUT_MS)
            ShellHandle(channel, input, output)
        } catch (t: Throwable) {
            runCatching { channel?.disconnect() }
            invalidate(server.id)
            throw t
        }
    }

    fun invalidate(serverId: String) {
        sessions.remove(serverId)?.let { runCatching { it.disconnect() } }
    }

    fun disconnectAll() {
        sessions.values.forEach { runCatching { it.disconnect() } }
        sessions.clear()
    }

    private suspend fun sessionFor(server: ServerConfig): Session {
        sessions[server.id]?.takeIf { it.isConnected }?.let { return it }
        return lockFor(server.id).withLock {
            sessions[server.id]?.takeIf { it.isConnected }?.let { return@withLock it }
            val credentials = repository.credentials(server)
            val connected = connectSession(server, credentials, persistFingerprint = true)
            sessions[server.id] = connected.session
            connected.session
        }
    }

    private fun lockFor(id: String): Mutex = locks.computeIfAbsent(id) { Mutex() }

    private suspend fun connectSession(
        server: ServerConfig,
        credentials: Credentials,
        persistFingerprint: Boolean,
    ): ConnectedSession {
        if (server.host.isBlank()) throw IllegalArgumentException("服务器地址为空")
        if (server.username.isBlank()) throw IllegalArgumentException("用户名为空")

        val password = credentials.password.orEmpty()
        val privateKey = credentials.privateKey
        val passphrase = credentials.passphrase

        when (server.authType) {
            com.watchcat.app.data.AuthType.PASSWORD -> {
                if (password.isEmpty()) throw IllegalArgumentException("未配置 SSH 密码")
            }
            com.watchcat.app.data.AuthType.PRIVATE_KEY -> {
                if (privateKey.isNullOrBlank()) throw IllegalArgumentException("未配置 SSH 私钥")
            }
        }

        val jsch = JSch()
        JschSupport.configure()
        val hostKeyRepository = TofuHostKeyRepository(server.fingerprint)
        jsch.hostKeyRepository = hostKeyRepository

        if (server.authType == com.watchcat.app.data.AuthType.PRIVATE_KEY && !privateKey.isNullOrBlank()) {
            try {
                jsch.addIdentity(
                    "watchcat-${server.id}",
                    privateKey.toByteArray(Charsets.UTF_8),
                    null,
                    passphrase?.takeIf { it.isNotEmpty() }?.toByteArray(Charsets.UTF_8),
                )
            } catch (t: Throwable) {
                throw IllegalArgumentException("私钥解析失败：请确认格式为 OpenSSH / PEM，且口令正确", t)
            }
        }

        val session = jsch.getSession(server.username.trim(), server.host.trim(), server.port)
        session.setConfig("StrictHostKeyChecking", "yes")
        session.setConfig(
            "PreferredAuthentications",
            if (server.authType == com.watchcat.app.data.AuthType.PRIVATE_KEY) {
                "publickey,keyboard-interactive,password"
            } else {
                "keyboard-interactive,password,publickey"
            },
        )
        session.setConfig("HashKnownHosts", "no")
        session.serverAliveInterval = 20_000
        session.serverAliveCountMax = 3
        session.userInfo = object : UserInfo {
            override fun getPassphrase(): String = passphrase.orEmpty()
            override fun getPassword(): String = password
            override fun promptPassword(message: String?): Boolean = password.isNotEmpty()
            override fun promptPassphrase(message: String?): Boolean = !passphrase.isNullOrEmpty()
            override fun promptYesNo(message: String?): Boolean = false
            override fun showMessage(message: String?) = Unit
        }

        if (password.isNotEmpty()) session.setPassword(password.toByteArray(Charsets.UTF_8))

        try {
            session.timeout = CONNECT_TIMEOUT_MS
            session.connect(CONNECT_TIMEOUT_MS)
            session.timeout = 0
        } catch (t: JSchException) {
            runCatching { session.disconnect() }
            if (hostKeyRepository.changed) {
                throw HostKeyChangedException(server.fingerprint, hostKeyRepository.actualFingerprint)
            }
            throw t
        } catch (t: Throwable) {
            runCatching { session.disconnect() }
            throw t
        }

        val fingerprint = hostKeyRepository.actualFingerprint
        val hostKeyType = runCatching { session.hostKey?.type }.getOrNull()

        if (server.fingerprint == null && fingerprint != null && persistFingerprint) {
            repository.updateFingerprint(server.id, fingerprint)
        }

        return ConnectedSession(session, fingerprint, hostKeyType)
    }

    private fun runExec(session: Session, command: String, timeoutMs: Int): String {
        var channel: ChannelExec? = null
        try {
            channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.setInputStream(null)
            val stderr = ByteArrayOutputStream()
            channel.setErrStream(stderr, false)
            val input = channel.inputStream
            channel.connect(CONNECT_TIMEOUT_MS)

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            val deadline = System.currentTimeMillis() + timeoutMs
            while (true) {
                var readSomething = false
                while (input.available() > 0) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    readSomething = true
                }
                if (channel.isClosed && input.available() == 0) break
                if (System.currentTimeMillis() > deadline) {
                    throw SocketTimeoutException("命令执行超时")
                }
                if (!readSomething) Thread.sleep(15)
            }

            val exit = channel.exitStatus
            if (exit != 0 && output.size() == 0) {
                val err = stderr.toString(Charsets.UTF_8.name()).trim()
                throw IllegalStateException(err.ifBlank { "命令执行失败（exit $exit）" })
            }
            return output.toString(Charsets.UTF_8.name())
        } finally {
            runCatching { channel?.disconnect() }
        }
    }

    private class ConnectedSession(
        val session: Session,
        val fingerprint: String?,
        val hostKeyType: String?,
    )

    private class TofuHostKeyRepository(private val expected: String?) : HostKeyRepository {
        var actualFingerprint: String? = null
            private set
        var changed: Boolean = false
            private set

        override fun check(host: String, key: ByteArray): Int {
            val actual = fingerprint(key)
            actualFingerprint = actual
            return when {
                expected == null -> HostKeyRepository.OK
                expected == actual -> HostKeyRepository.OK
                else -> {
                    changed = true
                    HostKeyRepository.CHANGED
                }
            }
        }

        override fun add(hostkey: HostKey?, ui: UserInfo?) = Unit
        override fun remove(host: String?, type: String?) = Unit
        override fun remove(host: String?, type: String?, key: ByteArray?) = Unit
        override fun getKnownHostsRepositoryID(): String = "WatchCat"
        override fun getHostKey(): Array<HostKey> = emptyArray()
        override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 9_000
        private const val SHELL_TIMEOUT_MS = 10_000

        fun fingerprint(key: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(key)
            return "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING)
        }

        private fun hostKeyTypeName(type: Int): String = when (type) {
            HostKey.SSHRSA -> "ssh-rsa"
            HostKey.ECDSA256 -> "ecdsa-sha2-nistp256"
            HostKey.ECDSA384 -> "ecdsa-sha2-nistp384"
            HostKey.ECDSA521 -> "ecdsa-sha2-nistp521"
            HostKey.ED25519 -> "ssh-ed25519"
            HostKey.ED448 -> "ssh-ed448"
            else -> "unknown"
        }

        private fun shellQuote(value: String): String =
            "'" + value.replace("'", "'\\''") + "'"
    }
}

fun humanReadableSshError(t: Throwable): String {
    if (t is HostKeyChangedException) {
        return "主机指纹已变化（可能重装或遭到中间人攻击）。\n当前指纹：${t.actual ?: "未知"}\n如确认安全，请在服务器详情中重置指纹。"
    }
    if (t is IllegalArgumentException) {
        return t.message ?: "配置不完整"
    }
    if (t is SocketTimeoutException) {
        return "连接或命令执行超时，请检查网络与服务器负载"
    }
    val message = t.message.orEmpty()
    return when {
        message.contains("Auth fail", true) ||
            message.contains("auth cancel", true) ||
            message.contains("Auth cancel", true) ->
            "认证失败：请检查用户名、密码或密钥"
        message.contains("UnknownHost", true) -> "无法解析主机名"
        message.contains("Connection refused", true) -> "连接被拒绝：端口未开放或被防火墙拦截"
        message.contains("No route to host", true) -> "网络不可达"
        message.contains("timed out", true) || message.contains("timeout", true) -> "连接超时，请检查 IP、端口与防火墙"
        message.contains("invalid privatekey", true) ||
            message.contains("Private key", true) ||
            message.contains("passphrase", true) -> "私钥无法使用：请检查格式与口令"
        message.contains("Algorithm negotiation fail", true) -> "SSH 加密算法协商失败"
        message.contains("HostKey", true) -> "主机密钥校验失败"
        message.contains("Connection reset", true) -> "连接被服务器重置"
        else -> message.ifBlank { t.javaClass.simpleName }
    }
}
