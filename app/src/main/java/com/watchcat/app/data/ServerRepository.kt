package com.watchcat.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ServerRepository(context: Context) {
    private val appContext = context.applicationContext
    private val serversFile = File(appContext.filesDir, "servers.json")
    private val settingsFile = File(appContext.filesDir, "settings.json")
    private val cipher = SecretCipher()
    private val ioMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val serversSerializer = ListSerializer(ServerConfig.serializer())

    private val _servers = MutableStateFlow<List<ServerConfig>>(emptyList())
    val servers: StateFlow<List<ServerConfig>> = _servers.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var loaded = false

    suspend fun load() = ioMutex.withLock {
        if (loaded) return@withLock
        withContext(Dispatchers.IO) {
            val decodedServers = runCatching {
                if (serversFile.exists()) {
                    json.decodeFromString(serversSerializer, serversFile.readText())
                } else {
                    null
                }
            }.getOrNull()

            if (decodedServers != null) {
                _servers.value = decodedServers
            } else {
                val demo = DemoData.server()
                _servers.value = listOf(demo)
                writeServers(_servers.value)
            }

            val decodedSettings = runCatching {
                if (settingsFile.exists()) {
                    json.decodeFromString(AppSettings.serializer(), settingsFile.readText())
                } else {
                    null
                }
            }.getOrNull()
            if (decodedSettings != null) _settings.value = decodedSettings
            loaded = true
        }
    }

    fun get(id: String): ServerConfig? = _servers.value.firstOrNull { it.id == id }

    suspend fun upsert(
        config: ServerConfig,
        password: String? = null,
        privateKey: String? = null,
        passphrase: String? = null,
        replaceSecrets: Boolean = false,
    ): Result<ServerConfig> = runCatching {
        val existing = get(config.id)
        val finalConfig = withContext(Dispatchers.IO) {
            val passwordEnc = when {
                replaceSecrets && config.authType == AuthType.PASSWORD ->
                    cipher.encrypt(password)
                replaceSecrets -> null
                config.authType == AuthType.PASSWORD && !password.isNullOrEmpty() ->
                    cipher.encrypt(password)
                existing?.authType == AuthType.PASSWORD -> existing.passwordEnc
                else -> null
            }
            val keyEnc = when {
                replaceSecrets && config.authType == AuthType.PRIVATE_KEY ->
                    cipher.encrypt(privateKey)
                replaceSecrets -> null
                config.authType == AuthType.PRIVATE_KEY && !privateKey.isNullOrEmpty() ->
                    cipher.encrypt(privateKey)
                existing?.authType == AuthType.PRIVATE_KEY -> existing.privateKeyEnc
                else -> null
            }
            val keyReplaced = replaceSecrets || !privateKey.isNullOrEmpty()
            val passphraseEnc = when {
                config.authType != AuthType.PRIVATE_KEY -> null
                keyReplaced -> cipher.encrypt(passphrase)
                !passphrase.isNullOrEmpty() -> cipher.encrypt(passphrase)
                existing?.authType == AuthType.PRIVATE_KEY -> existing.passphraseEnc
                else -> null
            }
            config.copy(
                passwordEnc = passwordEnc,
                privateKeyEnc = keyEnc,
                passphraseEnc = passphraseEnc,
            )
        }

        ioMutex.withLock {
            val next = _servers.value.toMutableList()
            val index = next.indexOfFirst { it.id == finalConfig.id }
            if (index >= 0) next[index] = finalConfig else next.add(finalConfig)
            writeServers(next)
            _servers.value = next
        }
        finalConfig
    }

    suspend fun delete(id: String): Unit = ioMutex.withLock {
        val next = _servers.value.filterNot { it.id == id }
        writeServers(next)
        _servers.value = next
    }

    suspend fun updateFingerprint(id: String, fingerprint: String?) {
        val current = get(id) ?: return
        val updated = current.copy(fingerprint = fingerprint)
        ioMutex.withLock {
            val next = _servers.value.map { if (it.id == id) updated else it }
            writeServers(next)
            _servers.value = next
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        ioMutex.withLock {
            withContext(Dispatchers.IO) {
                atomicWrite(settingsFile, json.encodeToString(AppSettings.serializer(), settings))
            }
        }
    }

    suspend fun credentials(server: ServerConfig): Credentials = withContext(Dispatchers.IO) {
        Credentials(
            password = cipher.decrypt(server.passwordEnc),
            privateKey = cipher.decrypt(server.privateKeyEnc),
            passphrase = cipher.decrypt(server.passphraseEnc),
        )
    }

    fun encryptSecret(value: String?): String? = cipher.encrypt(value)

    fun decryptSecret(value: String?): String? = cipher.decrypt(value)

    fun newId(): String = UUID.randomUUID().toString()

    private suspend fun writeServers(list: List<ServerConfig>) = withContext(Dispatchers.IO) {
        atomicWrite(serversFile, json.encodeToString(serversSerializer, list))
    }

    private fun atomicWrite(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            target.writeText(content)
            tmp.delete()
        }
    }
}
