package dev.tsdroid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val KEY_AUDIO_GAIN = floatPreferencesKey("audio_gain")
private val KEY_SHOW_LINK_THUMBNAILS = booleanPreferencesKey("show_link_thumbnails")
private val KEY_AUTO_LOAD_IMAGES = booleanPreferencesKey("auto_load_images")
private val KEY_IS_PTT_MODE = booleanPreferencesKey("is_ptt_mode")
private val KEY_VAD_THRESHOLD = floatPreferencesKey("vad_threshold")
private val KEY_NOISE_SUPPRESSION = booleanPreferencesKey("noise_suppression")
private val KEY_ECHO_CANCELLATION = booleanPreferencesKey("echo_cancellation")

class SettingsStore(private val context: Context) {

    val audioGain: Flow<Float> = context.settingsDataStore.data
        .map { it[KEY_AUDIO_GAIN] ?: 1.0f }

    val showLinkThumbnails: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_SHOW_LINK_THUMBNAILS] ?: false }

    val autoLoadImages: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_AUTO_LOAD_IMAGES] ?: true }

    val isPttMode: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_IS_PTT_MODE] ?: true }

    val vadThreshold: Flow<Float> = context.settingsDataStore.data
        .map { it[KEY_VAD_THRESHOLD] ?: 0.25f }

    val noiseSuppressionEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_NOISE_SUPPRESSION] ?: true }

    val echoCancellationEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_ECHO_CANCELLATION] ?: true }

    suspend fun setAudioGain(gain: Float) {
        context.settingsDataStore.edit { it[KEY_AUDIO_GAIN] = gain }
    }

    suspend fun setShowLinkThumbnails(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SHOW_LINK_THUMBNAILS] = enabled }
    }

    suspend fun setAutoLoadImages(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_AUTO_LOAD_IMAGES] = enabled }
    }

    suspend fun setIsPttMode(isPtt: Boolean) {
        context.settingsDataStore.edit { it[KEY_IS_PTT_MODE] = isPtt }
    }

    suspend fun setVadThreshold(threshold: Float) {
        context.settingsDataStore.edit { it[KEY_VAD_THRESHOLD] = threshold.coerceIn(0.01f, 1.0f) }
    }

    suspend fun setNoiseSuppressionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOISE_SUPPRESSION] = enabled }
    }

    suspend fun setEchoCancellationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ECHO_CANCELLATION] = enabled }
    }
}
