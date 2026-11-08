package dev.tsdroid.ui.component

import android.media.MediaRecorder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tsdroid.R
import dev.tsdroid.bridge.InputDeviceInfo

@Composable
fun AppSettingsDialog(
    appLanguage: String,
    onAppLanguageChange: (String) -> Unit,
    isPttMode: Boolean,
    onPttModeChange: (Boolean) -> Unit,
    vadThreshold: Float,
    onVadThresholdChange: (Float) -> Unit,
    micLevel: Float = 0f,
    noiseSuppressionEnabled: Boolean,
    onNoiseSuppressionChange: (Boolean) -> Unit,
    echoCancellationEnabled: Boolean,
    onEchoCancellationChange: (Boolean) -> Unit,
    audioSource: Int,
    onAudioSourceChange: (Int) -> Unit,
    preferredDeviceId: Int = -1,
    onPreferredDeviceIdChange: ((Int) -> Unit)? = null,
    availableDevices: List<InputDeviceInfo> = emptyList(),
    currentGain: Float? = null,
    onGainChange: ((Float) -> Unit)? = null,
    showLinkThumbnails: Boolean,
    onShowLinkThumbnailsChange: (Boolean) -> Unit,
    autoLoadImages: Boolean,
    onAutoLoadImagesChange: (Boolean) -> Unit,
    autoReconnect: Boolean? = null,
    onAutoReconnectChange: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var vadSlider by remember(vadThreshold) { mutableFloatStateOf(vadThreshold) }
    var gainSlider by remember(currentGain) { mutableFloatStateOf(currentGain ?: 1.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Section: App Language
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val languages = listOf(
                        "system" to stringResource(R.string.language_system),
                        "ru" to stringResource(R.string.language_ru),
                        "en" to stringResource(R.string.language_en),
                    )
                    languages.forEach { (code, label) ->
                        FilterChip(
                            selected = appLanguage == code,
                            onClick = { onAppLanguageChange(code) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Section: Voice Transmission Mode
                Text(
                    text = stringResource(R.string.transmission_mode),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = isPttMode,
                        onClick = { onPttModeChange(true) },
                        label = { Text("PTT") },
                        leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = !isPttMode,
                        onClick = { onPttModeChange(false) },
                        label = { Text("VAD") },
                        leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))

                // VAD Sensitivity & Live Level Bar
                Text(
                    text = "${stringResource(R.string.vad_sensitivity)}: ${(vadSlider * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))

                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp)),
                ) {
                    drawRect(color = trackColor)
                    val levelW = size.width * micLevel.coerceIn(0f, 1f)
                    val activeColor = if (micLevel >= vadSlider) Color(0xFF4CAF50) else primaryColor
                    drawRect(color = activeColor, size = Size(levelW, size.height))
                    val thresholdX = size.width * vadSlider.coerceIn(0f, 1f)
                    drawLine(
                        color = Color(0xFFFF5252),
                        start = Offset(thresholdX, 0f),
                        end = Offset(thresholdX, size.height),
                        strokeWidth = 3.dp.toPx(),
                    )
                }

                Slider(
                    value = vadSlider,
                    onValueChange = { vadSlider = it },
                    onValueChangeFinished = { onVadThresholdChange(vadSlider) },
                    valueRange = 0.05f..0.95f,
                )

                Spacer(Modifier.height(12.dp))

                // Section: Microphone Audio Source
                Text(
                    text = stringResource(R.string.audio_source),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))

                val sources = listOf(
                    MediaRecorder.AudioSource.MIC to stringResource(R.string.audio_source_mic),
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION to stringResource(R.string.audio_source_voice_communication),
                    MediaRecorder.AudioSource.UNPROCESSED to stringResource(R.string.audio_source_unprocessed),
                    MediaRecorder.AudioSource.CAMCORDER to stringResource(R.string.audio_source_camcorder),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sources.forEach { (srcId, srcLabel) ->
                        FilterChip(
                            selected = audioSource == srcId,
                            onClick = { onAudioSourceChange(srcId) },
                            label = { Text(srcLabel, style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = if (audioSource == srcId) {
                                { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Section: Input Device (Physical microphone)
                if (onPreferredDeviceIdChange != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.input_device),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = preferredDeviceId == -1,
                            onClick = { onPreferredDeviceIdChange(-1) },
                            label = { Text(stringResource(R.string.device_default), style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = if (preferredDeviceId == -1) {
                                { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        availableDevices.forEach { dev ->
                            FilterChip(
                                selected = preferredDeviceId == dev.id,
                                onClick = { onPreferredDeviceIdChange(dev.id) },
                                label = { Text(dev.name, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = if (preferredDeviceId == dev.id) {
                                    { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Section: Audio Processing (AEC & NS)
                Text(
                    text = stringResource(R.string.audio_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.echo_cancellation),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = echoCancellationEnabled,
                        onCheckedChange = onEchoCancellationChange,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.noise_suppression),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = noiseSuppressionEnabled,
                        onCheckedChange = onNoiseSuppressionChange,
                    )
                }

                if (onGainChange != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.audio_gain)}: ${stringResource(R.string.audio_gain_value, gainSlider)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = gainSlider,
                        onValueChange = { gainSlider = it },
                        onValueChangeFinished = { onGainChange(gainSlider) },
                        valueRange = 1.0f..8.0f,
                        steps = 13,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Section: General Media Settings
                Text(
                    text = stringResource(R.string.general_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))

                if (autoReconnect != null && onAutoReconnectChange != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.auto_reconnect),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = autoReconnect,
                            onCheckedChange = onAutoReconnectChange,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.show_link_thumbnails),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showLinkThumbnails,
                        onCheckedChange = onShowLinkThumbnailsChange,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.auto_load_images),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = autoLoadImages,
                        onCheckedChange = onAutoLoadImagesChange,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
