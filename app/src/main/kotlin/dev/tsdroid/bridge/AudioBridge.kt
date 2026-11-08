package dev.tsdroid.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dev.tsdroid.R
import dev.tslib.AudioConfig
import dev.tslib.OpusCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

data class InputDeviceInfo(
    val id: Int,
    val name: String,
    val typeName: String,
)

class AudioBridge(
    private val context: Context,
    private val tsClient: TsClient,
) {
    companion object {
        private const val TAG = "AudioBridge"
        const val SAMPLE_RATE = 48000
        const val CODEC_OPUS_VOICE = 4
        private const val FRAME_SIZE_MS = 20
        private const val FRAME_SIZE_SAMPLES = SAMPLE_RATE * FRAME_SIZE_MS / 1000 // 960
        private const val FRAME_SIZE_BYTES = FRAME_SIZE_SAMPLES * 2 // 16-bit PCM = 2 bytes/sample
        private const val MAX_QUEUE_FRAMES = 10 // Max buffered frames per user
        private const val HANGOVER_FRAMES = 15 // 15 * 20ms = 300ms hangover for smooth speech
    }

    private val audioConfig = AudioConfig()

    // Encoder for capture (our mic)
    private var encoder: OpusCodec? = null

    // Per-user decoders and raw opus queues for playback mixing
    private val userDecoders = ConcurrentHashMap<Int, OpusCodec>()
    private val userQueues = ConcurrentHashMap<Int, ArrayDeque<ByteArray>>()
    private val userVolumes = ConcurrentHashMap<Int, Float>()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    // Hardware Audio Effects
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null

    @Volatile
    var isAecEnabled: Boolean = true
        set(value) {
            field = value
            try { aec?.enabled = value } catch (_: Exception) {}
        }

    @Volatile
    var isNsEnabled: Boolean = true
        set(value) {
            field = value
            try { ns?.enabled = value } catch (_: Exception) {}
        }

    private var captureJob: Job? = null
    private var playbackJob: Job? = null

    // Dedicated single-thread scope for audio playback
    @OptIn(ExperimentalCoroutinesApi::class)
    private val playbackScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )

    @Volatile
    var gainFactor: Float = 1.0f

    @Volatile
    var isVoiceActivationMode: Boolean = false

    @Volatile
    var isPttPressed: Boolean = false

    @Volatile
    var vadThreshold: Float = 0.25f

    @Volatile
    var audioSource: Int = MediaRecorder.AudioSource.MIC
        set(value) {
            if (field != value) {
                field = value
                restartCapture()
            }
        }

    @Volatile
    var preferredInputDeviceId: Int = -1
        set(value) {
            if (field != value) {
                field = value
                restartCapture()
            }
        }

    private var captureScope: CoroutineScope? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerphoneOn = MutableStateFlow(true)
    val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // Real-time microphone input level (0.0 to 1.0)
    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    // Whether audio is currently being transmitted
    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive: StateFlow<Boolean> = _isVoiceActive.asStateFlow()

    fun initialize() {
        encoder = OpusCodec(audioConfig)
        applySpeakerphoneRouting(_isSpeakerphoneOn.value)
        initAudioTrack(_isSpeakerphoneOn.value)
        startPlaybackLoop()
    }

    @SuppressLint("MissingPermission")
    fun startCapture(scope: CoroutineScope) {
        if (_isCapturing.value) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return

        captureScope = scope

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val record = AudioRecord(
            audioSource,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, FRAME_SIZE_BYTES * 4),
        )

        if (preferredInputDeviceId != -1) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val dev = audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)?.find { it.id == preferredInputDeviceId }
            if (dev != null) {
                record.setPreferredDevice(dev)
            }
        }

        audioRecord = record

        // Attach hardware audio effects if supported
        initAudioEffects(record.audioSessionId)

        record.startRecording()
        _isCapturing.value = true

        // Ensure proper audio routing after recording has started
        applySpeakerphoneRouting(_isSpeakerphoneOn.value)

        captureJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(FRAME_SIZE_SAMPLES)
            val codec = encoder ?: return@launch
            var hangoverRemaining = 0

            while (isActive && _isCapturing.value) {
                val read = audioRecord?.read(buffer, 0, FRAME_SIZE_SAMPLES) ?: break
                if (read == FRAME_SIZE_SAMPLES) {
                    // Apply microphone gain
                    val gain = gainFactor
                    if (gain != 1.0f) {
                        for (i in 0 until read) {
                            buffer[i] = (buffer[i] * gain).toInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        }
                    }

                    // Compute RMS energy on amplified buffer
                    var sumSquares = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i].toDouble()
                        sumSquares += s * s
                    }
                    val rms = sqrt(sumSquares / read)
                    val normalizedLevel = (rms / 32768.0).coerceIn(0.0, 1.0).toFloat()
                    _micLevel.value = normalizedLevel

                    // Determine transmission state
                    val shouldTransmit: Boolean
                    if (_isMuted.value) {
                        shouldTransmit = false
                    } else if (isVoiceActivationMode) {
                        if (normalizedLevel >= vadThreshold) {
                            hangoverRemaining = HANGOVER_FRAMES
                        } else if (hangoverRemaining > 0) {
                            hangoverRemaining--
                        }
                        shouldTransmit = hangoverRemaining > 0
                    } else {
                        // PTT mode: transmit whenever PTT button is pressed
                        shouldTransmit = isPttPressed
                    }

                    if (_isVoiceActive.value != shouldTransmit) {
                        _isVoiceActive.value = shouldTransmit
                    }

                    if (shouldTransmit) {
                        val pcmBytes = shortsToBytes(buffer)
                        try {
                            val encoded = codec.encode(pcmBytes)
                            tsClient.sendAudio(encoded, CODEC_OPUS_VOICE)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun initAudioEffects(audioSessionId: Int) {
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioSessionId)?.apply {
                    enabled = isAecEnabled
                }
                Log.i(TAG, "AcousticEchoCanceler initialized: enabled=${aec?.enabled}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize AcousticEchoCanceler", e)
        }

        try {
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(audioSessionId)?.apply {
                    enabled = isNsEnabled
                }
                Log.i(TAG, "NoiseSuppressor initialized: enabled=${ns?.enabled}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize NoiseSuppressor", e)
        }
    }

    private fun releaseAudioEffects() {
        try {
            aec?.release()
            aec = null
        } catch (_: Exception) {}
        try {
            ns?.release()
            ns = null
        } catch (_: Exception) {}
    }

    fun stopCapture() {
        _isCapturing.value = false
        _micLevel.value = 0f
        _isVoiceActive.value = false
        captureJob?.cancel()
        captureJob = null
        releaseAudioEffects()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    @Synchronized
    private fun initAudioTrack(speakerphone: Boolean = _isSpeakerphoneOn.value) {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        // For speakerphone (loud speakers), USAGE_MEDIA guarantees Android plays through external loud speakers!
        // For earpiece (in-ear), USAGE_VOICE_COMMUNICATION routes to the in-call earpiece.
        val usage = if (speakerphone) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_VOICE_COMMUNICATION

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, FRAME_SIZE_BYTES * 4))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            val targetType = if (speakerphone) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            val targetDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type == targetType }
            if (targetDevice != null) {
                track.setPreferredDevice(targetDevice)
            }
        }

        track.play()
        audioTrack = track
    }

    /**
     * Playback loop: decodes one opus frame per user, applies per-user volume,
     * mixes PCM, and writes one combined frame to AudioTrack.
     */
    private fun startPlaybackLoop() {
        playbackJob = playbackScope.launch {
            val mixBuffer = ShortArray(FRAME_SIZE_SAMPLES)
            val decodeBuffer = ShortArray(FRAME_SIZE_SAMPLES)

            while (isActive) {
                var hasData = false
                mixBuffer.fill(0)

                for ((userId, queue) in userQueues) {
                    val opusData = synchronized(queue) { queue.pollFirst() } ?: continue
                    val userVol = userVolumes[userId] ?: 1.0f
                    if (userVol <= 0.001f) {
                        // User muted individually - skip decoding to save CPU
                        continue
                    }

                    val decoder = userDecoders.getOrPut(userId) { OpusCodec(audioConfig) }
                    try {
                        val pcmBytes = decoder.decode(opusData)
                        bytesToShorts(pcmBytes, decodeBuffer)
                        hasData = true

                        // Mix: apply individual volume + sum with clipping
                        for (i in mixBuffer.indices) {
                            val userSample = (decodeBuffer[i] * userVol).toInt()
                            val sum = mixBuffer[i].toInt() + userSample
                            mixBuffer[i] = sum.coerceIn(
                                Short.MIN_VALUE.toInt(),
                                Short.MAX_VALUE.toInt(),
                            ).toShort()
                        }
                    } catch (_: Exception) {}
                }

                if (hasData) {
                    val bytes = shortsToBytes(mixBuffer)
                    try {
                        audioTrack?.write(bytes, 0, bytes.size)
                    } catch (_: Exception) {}
                } else {
                    // No audio data — sleep briefly to avoid busy-waiting
                    delay(5)
                }
            }
        }
    }

    /**
     * Queue an opus packet for a specific user.
     */
    fun playAudio(userId: Int, opusData: ByteArray) {
        val queue = userQueues.getOrPut(userId) { ArrayDeque() }
        synchronized(queue) {
            if (queue.size < MAX_QUEUE_FRAMES) {
                queue.addLast(opusData)
            }
        }
    }

    /**
     * Set individual volume multiplier for a user (0.0 to 2.0). 0.0 = muted.
     */
    fun setUserVolume(userId: Int, volume: Float) {
        userVolumes[userId] = volume.coerceIn(0.0f, 2.0f)
    }

    /**
     * Get individual volume multiplier for a user.
     */
    fun getUserVolume(userId: Int): Float = userVolumes[userId] ?: 1.0f

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            _isVoiceActive.value = false
        }
        tsClient.setInputMuted(muted)
    }

    fun toggleMute() {
        setMuted(!_isMuted.value)
    }

    fun restartCapture() {
        val scope = captureScope ?: return
        if (_isCapturing.value) {
            stopCapture()
            startCapture(scope)
        }
    }

    fun setSpeakerphoneOn(enabled: Boolean) {
        _isSpeakerphoneOn.value = enabled
        applySpeakerphoneRouting(enabled)
        initAudioTrack(enabled)
    }

    fun toggleSpeakerphone() {
        setSpeakerphoneOn(!_isSpeakerphoneOn.value)
    }

    fun applySpeakerphoneRouting(speakerphone: Boolean = _isSpeakerphoneOn.value) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (speakerphone) {
                audioManager.mode = AudioManager.MODE_NORMAL
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val targetDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (targetDevice != null) {
                        val res = audioManager.setCommunicationDevice(targetDevice)
                        Log.i(TAG, "setCommunicationDevice(SPEAKER): $res")
                    } else {
                        audioManager.clearCommunicationDevice()
                    }
                }
            } else {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val targetDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                    if (targetDevice != null) {
                        val res = audioManager.setCommunicationDevice(targetDevice)
                        Log.i(TAG, "setCommunicationDevice(EARPIECE): $res")
                    } else {
                        audioManager.clearCommunicationDevice()
                    }
                }
            }
            Log.i(TAG, "applySpeakerphoneRouting: speakerphone=$speakerphone, mode=${audioManager.mode}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply speakerphone routing", e)
        }
    }

    fun getAvailableInputDevices(): List<InputDeviceInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return emptyList()
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return devices.map { dev ->
            val typeName = when (dev.type) {
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> context.getString(R.string.mic_device_builtin)
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> context.getString(R.string.mic_device_wired_headset)
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> context.getString(R.string.mic_device_bluetooth)
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> context.getString(R.string.mic_device_usb)
                else -> context.getString(R.string.mic_device_other)
            }
            val devName = dev.productName?.toString().takeIf { !it.isNullOrBlank() } ?: typeName
            InputDeviceInfo(
                id = dev.id,
                name = "$devName ($typeName)",
                typeName = typeName,
            )
        }
    }

    fun release() {
        stopCapture()
        playbackJob?.cancel()
        playbackJob = null
        playbackScope.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        encoder?.close()
        encoder = null
        for (decoder in userDecoders.values) {
            try { decoder.close() } catch (_: Exception) {}
        }
        userDecoders.clear()
        userQueues.clear()
        userVolumes.clear()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager?.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (shorts[i].toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    private fun bytesToShorts(bytes: ByteArray, out: ShortArray) {
        val count = minOf(bytes.size / 2, out.size)
        for (i in 0 until count) {
            out[i] = ((bytes[i * 2].toInt() and 0xFF) or
                    (bytes[i * 2 + 1].toInt() shl 8)).toShort()
        }
    }
}
