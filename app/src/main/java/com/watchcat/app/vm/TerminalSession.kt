package com.watchcat.app.vm

import com.watchcat.app.ssh.ShellHandle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStreamReader

enum class TerminalState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
}

class TerminalSession(
    private val viewModel: AppViewModel,
    private val serverId: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val _state = MutableStateFlow(TerminalState.IDLE)
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val output: SharedFlow<String> = _output.asSharedFlow()

    private var handle: ShellHandle? = null
    private var connectJob: Job? = null
    private var readerJob: Job? = null

    @Volatile
    private var lastCols: Int = 80

    @Volatile
    private var lastRows: Int = 24

    fun connect(cols: Int = lastCols, rows: Int = lastRows) {
        lastCols = cols.coerceAtLeast(20)
        lastRows = rows.coerceAtLeast(5)
        readerJob?.cancel()
        closeHandle()
        _error.value = null
        _state.value = TerminalState.CONNECTING
        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                val opened = viewModel.openShell(serverId, lastCols, lastRows)
                handle = opened
                _state.value = TerminalState.CONNECTED
                readerJob = launch { readLoop(opened) }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                if (_state.value != TerminalState.DISCONNECTED) {
                    _state.value = TerminalState.FAILED
                    _error.value = viewModel.humanError(t)
                }
            }
        }
    }

    fun write(text: String) {
        if (text.isEmpty()) return
        scope.launch {
            writeMutex.withLock {
                runCatching {
                    handle?.output?.write(text.toByteArray(Charsets.UTF_8))
                    handle?.output?.flush()
                }
            }
        }
    }

    fun resize(cols: Int, rows: Int) {
        if (cols <= 0 || rows <= 0) return
        lastCols = cols
        lastRows = rows
        val current = handle ?: return
        scope.launch { current.resize(cols, rows) }
    }

    fun disconnect() {
        connectJob?.cancel()
        readerJob?.cancel()
        closeHandle()
        _state.value = TerminalState.DISCONNECTED
    }

    fun dispose() {
        connectJob?.cancel()
        readerJob?.cancel()
        closeHandle()
        scope.cancel()
    }

    private suspend fun readLoop(opened: ShellHandle) {
        val reader = InputStreamReader(opened.input, Charsets.UTF_8)
        val buffer = CharArray(4096)
        try {
            while (scope.isActive) {
                val count = reader.read(buffer)
                if (count < 0) break
                if (count > 0) {
                    _output.emit(String(buffer, 0, count))
                }
            }
        } catch (_: Throwable) {
            // 连接被主动关闭或网络中断
        } finally {
            if (handle === opened) {
                closeHandle()
                if (_state.value == TerminalState.CONNECTED) {
                    _state.value = TerminalState.DISCONNECTED
                }
            }
        }
    }

    private fun closeHandle() {
        val current = handle
        handle = null
        runCatching { current?.close() }
    }
}
