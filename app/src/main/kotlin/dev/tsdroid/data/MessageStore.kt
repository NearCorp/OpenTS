package dev.tsdroid.data

import android.content.Context
import android.util.Log
import dev.tsdroid.viewmodel.ChatMessage
import dev.tsdroid.viewmodel.FileAttachment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MessageStore(private val context: Context) {

    companion object {
        private const val TAG = "MessageStore"
        private const val MAX_MESSAGES = 500
    }

    private val messagesDir = File(context.filesDir, "messages")

    fun load(serverAddress: String): Pair<List<ChatMessage>, Map<Int, List<ChatMessage>>> {
        val file = fileFor(serverAddress)
        if (!file.exists()) return Pair(emptyList(), emptyMap())
        return try {
            val json = file.readText()
            parseServerMessages(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages for $serverAddress", e)
            Pair(emptyList(), emptyMap())
        }
    }

    fun save(
        serverAddress: String,
        channelMessages: List<ChatMessage>,
        privateMessages: Map<Int, List<ChatMessage>>,
    ) {
        try {
            messagesDir.mkdirs()
            val json = serializeServerMessages(channelMessages, privateMessages)
            fileFor(serverAddress).writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save messages for $serverAddress", e)
        }
    }

    private fun fileFor(serverAddress: String): File {
        return File(messagesDir, sanitizeFilename(serverAddress) + ".json")
    }

    private fun sanitizeFilename(address: String): String {
        return address.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    // --- Serialization ---

    private fun serializeServerMessages(
        channel: List<ChatMessage>,
        private_: Map<Int, List<ChatMessage>>,
    ): String {
        val root = JSONObject()
        val channelArray = JSONArray()
        channel.takeLast(MAX_MESSAGES).forEach { msg ->
            channelArray.put(serializeMessage(msg))
        }
        root.put("channel", channelArray)

        val privateObj = JSONObject()
        for ((userId, msgs) in private_) {
            val userArray = JSONArray()
            msgs.takeLast(MAX_MESSAGES).forEach { msg ->
                userArray.put(serializeMessage(msg))
            }
            privateObj.put(userId.toString(), userArray)
        }
        root.put("private", privateObj)
        return root.toString()
    }

    private fun serializeMessage(msg: ChatMessage): JSONObject {
        val obj = JSONObject()
        obj.put("s", msg.sender)
        obj.put("t", msg.text)
        obj.put("ts", msg.timestamp)
        obj.put("me", msg.isMe)
        obj.put("sid", msg.senderId)
        val fa = msg.fileAttachment
        if (fa != null) {
            val faObj = JSONObject()
            faObj.put("fn", fa.fileName)
            faObj.put("fs", fa.fileSize)
            faObj.put("fi", fa.fileId)
            faObj.put("im", fa.isImage)
            faObj.put("ch", fa.channelId)
            obj.put("fa", faObj)
        }
        return obj
    }

    // --- Parsing ---

    private fun parseServerMessages(json: String): Pair<List<ChatMessage>, Map<Int, List<ChatMessage>>> {
        if (json.isBlank()) return Pair(emptyList(), emptyMap())
        return try {
            val root = JSONObject(json)
            val channelMessages = mutableListOf<ChatMessage>()
            val privateMessages = mutableMapOf<Int, List<ChatMessage>>()

            val channelArray = root.optJSONArray("channel")
            if (channelArray != null) {
                for (i in 0 until channelArray.length()) {
                    channelArray.optJSONObject(i)?.let { msgObj ->
                        parseMessage(msgObj, isPrivate = false)?.let { channelMessages.add(it) }
                    }
                }
            }

            val privateObj = root.optJSONObject("private")
            if (privateObj != null) {
                val keys = privateObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val userId = key.toIntOrNull() ?: continue
                    val userArray = privateObj.optJSONArray(key) ?: continue
                    val userMsgs = mutableListOf<ChatMessage>()
                    for (i in 0 until userArray.length()) {
                        userArray.optJSONObject(i)?.let { msgObj ->
                            parseMessage(msgObj, isPrivate = true)?.let { userMsgs.add(it) }
                        }
                    }
                    privateMessages[userId] = userMsgs
                }
            }

            Pair(channelMessages, privateMessages)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse messages json", e)
            Pair(emptyList(), emptyMap())
        }
    }

    private fun parseMessage(obj: JSONObject, isPrivate: Boolean): ChatMessage? {
        val sender = obj.optString("s", "")
        val text = obj.optString("t", "")
        val ts = obj.optLong("ts", System.currentTimeMillis())
        val me = obj.optBoolean("me", false)
        val sid = obj.optInt("sid", 0)

        val faObj = obj.optJSONObject("fa")
        val fileAttachment = if (faObj != null) {
            val fn = faObj.optString("fn", "")
            val fs = faObj.optLong("fs", 0L)
            val fi = faObj.optString("fi", "")
            val im = faObj.optBoolean("im", false)
            val ch = faObj.optLong("ch", 0L)
            FileAttachment(fn, fs, fi, im, channelId = ch)
        } else null

        return ChatMessage(
            sender = sender,
            text = text,
            timestamp = ts,
            isMe = me,
            isPrivate = isPrivate,
            senderId = sid,
            fileAttachment = fileAttachment,
        )
    }
}
