package dev.tsdroid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

class BookmarkStore(private val context: Context) {

    companion object {
        private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks_json")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_LAST_BOOKMARK_ADDRESS = stringPreferencesKey("last_bookmark_address")
    }

    val bookmarks: Flow<List<ServerBookmark>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_BOOKMARKS] ?: "[]"
        parseBookmarks(json)
    }

    val autoReconnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_RECONNECT] ?: false
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_RECONNECT] = enabled
        }
    }

    suspend fun save(bookmarks: List<ServerBookmark>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BOOKMARKS] = serializeBookmarks(bookmarks)
        }
    }

    suspend fun add(bookmark: ServerBookmark) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]")
            prefs[KEY_BOOKMARKS] = serializeBookmarks(current + bookmark)
        }
    }

    suspend fun remove(index: Int) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]").toMutableList()
            if (index in current.indices) {
                current.removeAt(index)
                prefs[KEY_BOOKMARKS] = serializeBookmarks(current)
            }
        }
    }

    suspend fun replace(index: Int, bookmark: ServerBookmark) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]").toMutableList()
            if (index in current.indices) {
                // Preserve serverName and iconId from the old bookmark if not set
                val old = current[index]
                current[index] = bookmark.copy(
                    serverName = bookmark.serverName ?: old.serverName,
                    iconId = if (bookmark.iconId != 0L) bookmark.iconId else old.iconId,
                )
                prefs[KEY_BOOKMARKS] = serializeBookmarks(current)
            }
        }
    }

    /** Save the address of the last connected bookmark (for auto-reconnect). */
    suspend fun saveLastBookmarkAddress(address: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_BOOKMARK_ADDRESS] = address
        }
    }

    val lastBookmarkAddress: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BOOKMARK_ADDRESS] ?: ""
    }

    suspend fun updateServerInfo(address: String, serverName: String, iconId: Long) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]")
            val updated = current.map { b ->
                if (b.address == address) b.copy(serverName = serverName, iconId = iconId) else b
            }
            prefs[KEY_BOOKMARKS] = serializeBookmarks(updated)
        }
    }

    private fun parseBookmarks(jsonStr: String): List<ServerBookmark> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return try {
            val array = org.json.JSONArray(jsonStr)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                ServerBookmark(
                    name = obj.optString("name", ""),
                    address = obj.optString("address", ""),
                    nickname = obj.optString("nickname", ""),
                    password = if (obj.isNull("password") || obj.optString("password") == "null") null else obj.optString("password").takeIf { it.isNotEmpty() },
                    channel = if (obj.isNull("channel") || obj.optString("channel") == "null") null else obj.optString("channel").takeIf { it.isNotEmpty() },
                    serverName = if (obj.isNull("serverName") || obj.optString("serverName") == "null") null else obj.optString("serverName").takeIf { it.isNotEmpty() },
                    iconId = obj.optLong("iconId", 0L),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeBookmarks(bookmarks: List<ServerBookmark>): String {
        val array = org.json.JSONArray()
        for (b in bookmarks) {
            val obj = org.json.JSONObject()
            obj.put("name", b.name)
            obj.put("address", b.address)
            obj.put("nickname", b.nickname)
            if (b.password != null) obj.put("password", b.password)
            if (b.channel != null) obj.put("channel", b.channel)
            if (b.serverName != null) obj.put("serverName", b.serverName)
            obj.put("iconId", b.iconId)
            array.put(obj)
        }
        return array.toString()
    }
}
