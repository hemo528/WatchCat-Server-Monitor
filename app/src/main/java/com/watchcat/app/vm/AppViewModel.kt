package com.watchcat.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watchcat.app.data.AppSettings
import com.watchcat.app.data.AuthType
import com.watchcat.app.data.Credentials
import com.watchcat.app.data.DemoData
import com.watchcat.app.data.MetricSample
import com.watchcat.app.data.ServerConfig
import com.watchcat.app.data.ServerRepository
import com.watchcat.app.data.ServerStatus
import com.watchcat.app.data.ServerUiState
import com.watchcat.app.ssh.ConnectionTestResult
import com.watchcat.app.ssh.ShellHandle
import com.watchcat.app.ssh.SshManager
import com.watchcat.app.ssh.humanReadableSshError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.random.Random

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServerRepository(application)
    val ssh = SshManager(application, repository)

    val servers: StateFlow<List<ServerConfig>> = repository.servers
    val settings: StateFlow<AppSettings> = repository.settings

    private val _states = MutableStateFlow<Map<String, ServerUiState>>(emptyMap())
    val states: StateFlow<Map<String, ServerUiState>> = _states.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private var pollingJob: Job? = null
    private var manualJob: Job? = null
    private var foreground = false

    init {
        viewModelScope.launch {
            runCatching { repository.load() }
                .onFailure { _messages.tryEmit("读取本地数据失败：${it.message ?: "未知错误"}") }
            _loaded.value = true
            pruneStates()
            if (foreground) {
                pollingJob?.cancel()
                startPolling()
            }
        }
        viewModelScope.launch {
            repository.servers.collect { list ->
                val ids = list.map { it.id }.toSet()
                _states.update { map -> map.filterKeys { it in ids } }
            }
        }
    }

    fun onForeground() {
        if (foreground) return
        foreground = true
        startPolling()
    }

    fun onBackground() {
        foreground = false
        pollingJob?.cancel()
        pollingJob = null
        viewModelScope.launch(Dispatchers.IO) { ssh.disconnectAll() }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && foreground) {
                val list = repository.servers.value
                val started = System.currentTimeMillis()
                pollAll(list)
                val interval = (repository.settings.value.refreshSeconds.coerceIn(1.0, 300.0) * 1000.0).roundToLong()
                val elapsed = System.currentTimeMillis() - started
                delay(max(350L, interval - elapsed))
            }
        }
    }

    private suspend fun pollAll(list: List<ServerConfig>) = supervisorScope {
        val semaphore = Semaphore(4)
        list.forEach { server ->
            launch {
                semaphore.withPermit { pollServer(server) }
            }
        }
    }

    fun refreshAll() {
        if (manualJob?.isActive == true) return
        manualJob = viewModelScope.launch {
            pollAll(repository.servers.value)
        }
    }

    fun refreshServer(id: String) {
        val server = repository.get(id) ?: return
        viewModelScope.launch { pollServer(server, manual = true) }
    }

    private suspend fun pollServer(server: ServerConfig, manual: Boolean = false) {
        if (!inFlight.add(server.id)) return
        try {
            val previous = _states.value[server.id] ?: ServerUiState()
            _states.update {
                it + (server.id to previous.copy(
                    status = if (previous.metrics == null) ServerStatus.CONNECTING else previous.status,
                    isRefreshing = true,
                    error = null,
                ))
            }

            if (server.isDemo) {
                delay(260L + Random.nextLong(0, 240))
                val metrics = DemoData.metrics()
                applySuccess(server, metrics, Random.nextLong(8, 34))
                return
            }

            try {
                val result = ssh.fetchMetrics(server)
                applySuccess(server, result.metrics, result.latencyMs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _states.update { map ->
                    val current = map[server.id] ?: ServerUiState()
                    map + (server.id to current.copy(
                        status = ServerStatus.OFFLINE,
                        error = humanReadableSshError(error),
                        isRefreshing = false,
                        lastUpdated = System.currentTimeMillis(),
                    ))
                }
            }
        } finally {
            inFlight.remove(server.id)
            _states.update { map ->
                val current = map[server.id] ?: return@update map
                map + (server.id to current.copy(isRefreshing = false))
            }
        }
    }

    private fun applySuccess(server: ServerConfig, metrics: com.watchcat.app.data.Metrics, latencyMs: Long) {
        val sample = MetricSample(
            timestamp = System.currentTimeMillis(),
            cpuPercent = metrics.cpuPercent,
            memPercent = metrics.memUsedPercent,
            rxBytesPerSecond = metrics.rxBytesPerSecond,
            txBytesPerSecond = metrics.txBytesPerSecond,
        )
        _states.update { map ->
            val current = map[server.id] ?: ServerUiState()
            val history = (current.history + sample).takeLast(HISTORY_SIZE)
            map + (server.id to current.copy(
                status = ServerStatus.ONLINE,
                metrics = metrics,
                error = null,
                latencyMs = latencyMs,
                lastUpdated = System.currentTimeMillis(),
                history = history,
                isRefreshing = false,
            ))
        }
    }

    suspend fun testDraft(
        config: ServerConfig,
        password: String,
        privateKey: String,
        passphrase: String,
    ): Result<ConnectionTestResult> {
        val existing = repository.get(config.id)
        val existingCredentials = if (existing != null) {
            runCatching { repository.credentials(existing) }.getOrNull()
        } else {
            null
        }
        val credentials = if (config.authType == AuthType.PASSWORD) {
            Credentials(
                password = password.ifEmpty { existingCredentials?.password },
            )
        } else {
            Credentials(
                privateKey = privateKey.ifBlank { existingCredentials?.privateKey },
                passphrase = passphrase.ifEmpty { existingCredentials?.passphrase },
            )
        }
        val fingerprint = config.fingerprint ?: existing?.fingerprint
        return runCatching {
            ssh.testConnection(config.copy(fingerprint = fingerprint), credentials)
        }
    }

    suspend fun saveServer(
        config: ServerConfig,
        password: String,
        privateKey: String,
        passphrase: String,
        replaceSecrets: Boolean,
    ): Result<ServerConfig> {
        val existing = repository.get(config.id)
        val hostChanged = existing != null && (existing.host != config.host || existing.port != config.port)
        val prepared = config.copy(
            fingerprint = when {
                hostChanged -> null
                config.fingerprint != null -> config.fingerprint
                else -> existing?.fingerprint
            },
            createdAt = existing?.createdAt ?: config.createdAt,
        )
        return repository.upsert(
            config = prepared,
            password = password,
            privateKey = privateKey,
            passphrase = passphrase,
            replaceSecrets = replaceSecrets,
        ).onSuccess { saved ->
            ssh.invalidate(saved.id)
            _states.update { it - saved.id }
            viewModelScope.launch { pollServer(saved) }
        }.onFailure {
            _messages.tryEmit("保存失败：${it.message ?: "未知错误"}")
        }
    }

    suspend fun deleteServer(id: String): Result<Unit> = runCatching {
        ssh.invalidate(id)
        repository.delete(id)
        _states.update { it - id }
    }.onFailure {
        _messages.tryEmit("删除失败：${it.message ?: "未知错误"}")
    }

    suspend fun resetFingerprint(id: String) {
        repository.updateFingerprint(id, null)
        ssh.invalidate(id)
        _states.update { it - id }
        repository.get(id)?.let { pollServer(it) }
        _messages.tryEmit("已重置主机指纹，下次连接将重新信任")
    }

    suspend fun updateSettings(settings: AppSettings): Result<Unit> {
        val previous = repository.settings.value
        return runCatching {
            repository.updateSettings(settings)
        }.onSuccess {
            if (foreground && previous.refreshSeconds != settings.refreshSeconds) {
                pollingJob?.cancel()
                startPolling()
            }
        }
    }

    suspend fun addDemoServer(): Result<Unit> = runCatching {
        if (repository.servers.value.none { it.isDemo }) {
            repository.upsert(DemoData.server(), replaceSecrets = true)
            repository.servers.value.firstOrNull { it.isDemo }?.let { pollServer(it) }
        }
    }

    suspend fun credentialsFor(server: ServerConfig): Credentials = repository.credentials(server)

    fun decrypt(value: String?): String? = repository.decryptSecret(value)

    suspend fun openShell(serverId: String, cols: Int, rows: Int): ShellHandle {
        val server = repository.get(serverId) ?: throw IllegalStateException("服务器不存在")
        if (server.isDemo) throw IllegalStateException("演示服务器不支持 SSH")
        return ssh.openShell(server, cols, rows)
    }

    fun server(id: String): ServerConfig? = repository.get(id)

    fun humanError(t: Throwable): String = humanReadableSshError(t)

    fun toast(message: String) {
        _messages.tryEmit(message)
    }

    override fun onCleared() {
        super.onCleared()
        ssh.disconnectAll()
    }

    private fun pruneStates() {
        val ids = repository.servers.value.map { it.id }.toSet()
        _states.update { it.filterKeys { key -> key in ids } }
    }

    companion object {
        private const val HISTORY_SIZE = 60
    }
}
