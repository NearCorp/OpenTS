package dev.tsdroid.bridge

import android.util.Log
import dev.tslib.Channel
import dev.tslib.Client
import dev.tslib.ConnectionState
import dev.tslib.Event
import dev.tslib.Identity
import dev.tslib.ServerInfo
import dev.tslib.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

data class TsFileEntry(
    val name: String,
    val size: Long,
    val datetime: Long,
    val isFile: Boolean,
)

class TsClient {

    companion object {
        private const val TAG = "TsClient"
    }

    private val nativeLock = Any()
    private var client: Client? = null
    var serverAddress: String? = null
        private set

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<Int> = _state.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()

    private val _commandErrors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val commandErrors: SharedFlow<String> = _commandErrors.asSharedFlow()

    private val downloadCallbacks = ConcurrentHashMap<String, CompletableDeferred<ByteArray>>()
    private val uploadCallbacks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val fileListCallbacks = ConcurrentHashMap<String, CompletableDeferred<List<TsFileEntry>>>()

    private val eventLoopRunning = AtomicBoolean(false)

    val isConnected: Boolean
        get() = synchronized(nativeLock) { client?.isConnected == true }

    val clientId: Int?
        get() = synchronized(nativeLock) { client?.clientId }

    suspend fun connect(
        address: String,
        identity: Identity,
        nickname: String,
        password: String? = null,
        channel: String? = null,
    ) = withContext(Dispatchers.IO) {
        disconnect()
        serverAddress = address
        val c = synchronized(nativeLock) {
            val newClient = Client(address, identity, nickname, password, channel)
            client = newClient
            _state.value = ConnectionState.CONNECTING
            newClient.waitConnected()
            _state.value = ConnectionState.CONNECTED
            newClient
        }
        val users = synchronized(nativeLock) { c.users }
        val channels = synchronized(nativeLock) { c.channels }
        Log.i(TAG, "After waitConnected: ${users?.size ?: "null"} users, ${channels?.size ?: "null"} channels")
        refreshState()
    }

    suspend fun eventLoop() {
        if (!eventLoopRunning.compareAndSet(false, true)) return
        try {
            withContext(Dispatchers.IO) {
                var refreshCounter = 0
                while (coroutineContext.isActive && client != null) {
                    try {
                        val events = synchronized(nativeLock) {
                            val c = client ?: return@synchronized null
                            c.processEvents()
                        } ?: emptyArray()

                        for (event in events) {
                            _events.tryEmit(event)
                            handleEvent(event)
                        }
                        refreshCounter++
                        if (events.isNotEmpty() || refreshCounter >= 25) {
                            refreshState()
                            refreshCounter = 0
                        }
                    } catch (e: Exception) {
                        if (client == null) break
                        _state.value = ConnectionState.DISCONNECTED
                        break
                    }
                    delay(20)
                }
            }
        } finally {
            eventLoopRunning.set(false)
        }
    }

    private fun handleEvent(event: Event) {
        when (event.type) {
            "disconnected" -> _state.value = ConnectionState.DISCONNECTED
            "connected" -> _state.value = ConnectionState.CONNECTED
            "file_downloaded" -> {
                val path = event.data["path"] as? String ?: return
                val data = event.data["data"] as? ByteArray ?: return
                Log.d(TAG, "File downloaded: $path (${data.size} bytes)")
                downloadCallbacks.remove(path)?.complete(data)
            }
            "file_uploaded" -> {
                val path = event.data["path"] as? String ?: return
                Log.d(TAG, "File uploaded: $path")
                uploadCallbacks.remove(path)?.complete(true)
            }
            "file_transfer_failed" -> {
                val path = event.data["path"] as? String ?: return
                val error = event.data["error"] as? String ?: "unknown"
                Log.w(TAG, "File transfer failed: $path — $error")
                downloadCallbacks.remove(path)?.completeExceptionally(
                    Exception("File transfer failed: $error")
                )
                uploadCallbacks.remove(path)?.complete(false)
            }
            "file_list_received" -> {
                val channelId = (event.data["channel_id"] as? Number)?.toLong() ?: return
                val path = event.data["path"] as? String ?: return
                val filesJson = event.data["files"] as? String ?: return
                val entries = parseFileEntries(filesJson)
                Log.d(TAG, "File list received: channel=$channelId path=$path entries=${entries.size}")
                fileListCallbacks.remove("$channelId:$path")?.complete(entries)
            }
            "command_error" -> {
                val message = event.data["message"] as? String ?: return
                Log.w(TAG, "Command error: $message")
                _commandErrors.tryEmit(message)
            }
            "channel_permissions_updated" -> {
                val channelId = (event.data["channel_id"] as? Number)?.toLong() ?: return
                val hints = (event.data["permission_hints"] as? Number)?.toLong() ?: return
                Log.i(TAG, "Channel $channelId permissions updated: ${hints.toString(16)}")
                refreshState()
            }
        }
    }

    private fun refreshState() {
        try {
            val (chs, usrs, srv, st) = synchronized(nativeLock) {
                val c = client ?: return
                val channels = c.channels?.filterNotNull() ?: emptyList()
                val users = c.users?.filterNotNull() ?: emptyList()
                val sInfo = c.serverInfo
                val state = c.state
                Tuple4(channels, users, sInfo, state)
            } ?: return

            _channels.value = chs
            _users.value = usrs
            _serverInfo.value = srv
            if (_state.value != st) _state.value = st
        } catch (e: Exception) {
            Log.w(TAG, "refreshState failed", e)
        }
    }

    fun sendChannelMessage(msg: String) {
        synchronized(nativeLock) {
            client?.sendChannelMessage(msg)
        }
    }

    fun sendServerMessage(msg: String) {
        synchronized(nativeLock) {
            client?.sendServerMessage(msg)
        }
    }

    fun sendPrivateMessage(userId: Int, msg: String) {
        synchronized(nativeLock) {
            client?.sendPrivateMessage(userId, msg)
        }
    }

    fun moveToChannel(channelId: Long) {
        synchronized(nativeLock) {
            client?.moveToChannel(channelId)
        }
    }

    fun sendAudio(data: ByteArray, codec: Int) {
        synchronized(nativeLock) {
            client?.sendAudio(data, codec)
        }
    }

    fun setInputMuted(muted: Boolean) {
        synchronized(nativeLock) {
            val c = client ?: return
            try {
                c.setInputMuted(muted)
            } catch (e: Exception) {
                Log.w(TAG, "setInputMuted failed", e)
            }
        }
    }

    suspend fun downloadFile(channelId: Long, path: String): ByteArray? {
        val deferred = CompletableDeferred<ByteArray>()
        downloadCallbacks[path] = deferred
        try {
            val sent = synchronized(nativeLock) {
                client?.downloadFile(channelId, path) != null
            }
            if (!sent) {
                downloadCallbacks.remove(path)
                return null
            }
        } catch (e: Exception) {
            downloadCallbacks.remove(path)
            Log.w(TAG, "downloadFile failed for $path", e)
            return null
        }
        return withTimeoutOrNull(10_000) {
            try {
                deferred.await()
            } catch (e: Exception) {
                Log.w(TAG, "downloadFile await failed for $path", e)
                null
            }
        }.also { downloadCallbacks.remove(path) }
    }

    suspend fun listFiles(channelId: Long, path: String): List<TsFileEntry>? {
        val key = "$channelId:$path"
        val deferred = CompletableDeferred<List<TsFileEntry>>()
        fileListCallbacks[key] = deferred
        try {
            val sent = synchronized(nativeLock) {
                client?.listFiles(channelId, path) != null
            }
            if (!sent) {
                fileListCallbacks.remove(key)
                return null
            }
        } catch (e: Exception) {
            fileListCallbacks.remove(key)
            Log.w(TAG, "listFiles failed for $path", e)
            return null
        }
        return withTimeoutOrNull(5_000) {
            try { deferred.await() } catch (e: Exception) {
                Log.w(TAG, "listFiles await failed for $path", e)
                null
            }
        }.also { fileListCallbacks.remove(key) }
    }

    fun deleteFile(channelId: Long, name: String) {
        synchronized(nativeLock) {
            client?.deleteFile(channelId, name)
        }
    }

    fun renameFile(channelId: Long, oldName: String, newName: String) {
        synchronized(nativeLock) {
            client?.renameFile(channelId, oldName, newName)
        }
    }

    fun createDirectory(channelId: Long, dirname: String) {
        synchronized(nativeLock) {
            client?.createDirectory(channelId, dirname)
        }
    }

    fun queryChannelPermissions(channelId: Long) {
        synchronized(nativeLock) {
            client?.queryChannelPermissions(channelId)
        }
    }

    private fun parseFileEntries(json: String): List<TsFileEntry> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TsFileEntry(
                    name = obj.getString("name"),
                    size = obj.getLong("size"),
                    datetime = obj.getLong("datetime"),
                    isFile = obj.getBoolean("is_file"),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseFileEntries failed", e)
            emptyList()
        }
    }

    suspend fun uploadFile(channelId: Long, path: String, data: ByteArray, overwrite: Boolean = true): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        uploadCallbacks[path] = deferred
        try {
            val sent = synchronized(nativeLock) {
                client?.uploadFile(channelId, path, data, overwrite) != null
            }
            if (!sent) {
                uploadCallbacks.remove(path)
                return false
            }
        } catch (e: Exception) {
            uploadCallbacks.remove(path)
            Log.w(TAG, "uploadFile failed for $path", e)
            return false
        }
        return withTimeoutOrNull(30_000) {
            try {
                deferred.await()
            } catch (e: Exception) {
                Log.w(TAG, "uploadFile await failed for $path", e)
                false
            }
        }.also { uploadCallbacks.remove(path) } ?: false
    }

    fun disconnect() {
        val c = client ?: return
        client = null

        val deadline = System.currentTimeMillis() + 1000
        while (eventLoopRunning.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        eventLoopRunning.set(false)

        _state.value = ConnectionState.DISCONNECTED
        _channels.value = emptyList()
        _users.value = emptyList()
        _serverInfo.value = null

        synchronized(nativeLock) {
            try {
                c.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "disconnect failed", e)
            }

            try {
                val flushEnd = System.currentTimeMillis() + 300
                while (System.currentTimeMillis() < flushEnd) {
                    c.processEvents()
                    Thread.sleep(20)
                }
            } catch (_: Exception) {}

            try {
                c.close()
            } catch (_: Exception) {}
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
