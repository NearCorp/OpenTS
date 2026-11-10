package dev.tsdroid.ui.component

import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.tsdroid.BuildConfig
import dev.tsdroid.R
import dev.tsdroid.bridge.InputDeviceInfo
import java.io.File

@Composable
fun AppSettingsDialog(
    appLanguage: String,
    onAppLanguageChange: (String) -> Unit,
    isPttMode: Boolean,
    onPttModeChange: (Boolean) -> Unit,
    vadThreshold: Float,
    onVadThresholdChange: (Float) -> Unit,
    micLevel: Float = 0f,
    speakerphoneOn: Boolean = true,
    onSpeakerphoneOnChange: ((Boolean) -> Unit)? = null,
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
    customBackgroundPath: String = "",
    backgroundDim: Float = 0.45f,
    onSelectBackground: ((Uri?) -> Unit)? = null,
    onClearBackground: (() -> Unit)? = null,
    onBackgroundDimChange: ((Float) -> Unit)? = null,
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
    var dimSlider by remember(backgroundDim) { mutableFloatStateOf(backgroundDim) }

    val isAecSupported = remember {
        try { AcousticEchoCanceler.isAvailable() } catch (_: Exception) { false }
    }
    val isNsSupported = remember {
        try { NoiseSuppressor.isAvailable() } catch (_: Exception) { false }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectBackground?.invoke(uri)
        }
    }

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

                if (onSpeakerphoneOnChange != null) {
                    Spacer(Modifier.height(14.dp))

                    // Section: Audio Output (Speakerphone vs Earpiece)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.audio_output),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = speakerphoneOn,
                            onClick = { onSpeakerphoneOnChange(true) },
                            label = { Text(stringResource(R.string.output_speaker_short)) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = !speakerphoneOn,
                            onClick = { onSpeakerphoneOnChange(false) },
                            label = { Text(stringResource(R.string.output_earpiece_short)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
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

                // Section: Audio Processing (AEC & NS & Gain)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.echo_cancellation),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(if (isAecSupported) R.string.fx_hardware_supported else R.string.fx_hardware_unsupported),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAecSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                    Switch(
                        checked = echoCancellationEnabled,
                        onCheckedChange = onEchoCancellationChange,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.noise_suppression),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(if (isNsSupported) R.string.fx_hardware_supported else R.string.fx_hardware_unsupported),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNsSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
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

                // Section: Customization (Фон и оформление)
                if (onSelectBackground != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.customization),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    val hasCustomBg = customBackgroundPath.isNotBlank() && File(customBackgroundPath).exists()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.choose_background), style = MaterialTheme.typography.bodySmall)
                        }

                        if (hasCustomBg && onClearBackground != null) {
                            TextButton(
                                onClick = onClearBackground,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.remove_background),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (hasCustomBg) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                AsyncImage(
                                    model = File(customBackgroundPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${stringResource(R.string.background_dim)}: ${(dimSlider * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Slider(
                                    value = dimSlider,
                                    onValueChange = { dimSlider = it },
                                    onValueChangeFinished = { onBackgroundDimChange?.invoke(dimSlider) },
                                    valueRange = 0.1f..0.9f,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

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

                Spacer(Modifier.height(16.dp))

                // Section: About App (О программе)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.about_app),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "OpenTS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.about_creator),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.about_license),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.about_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.about_features_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        val features = listOf(
                            stringResource(R.string.about_feature_vad),
                            stringResource(R.string.about_feature_gain),
                            stringResource(R.string.about_feature_output),
                            stringResource(R.string.about_feature_fx),
                            stringResource(R.string.about_feature_users),
                            stringResource(R.string.about_feature_custom),
                            stringResource(R.string.about_feature_stream),
                        )
                        features.forEach { feat ->
                            Text(
                                text = feat,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.about_stack),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.about_cute_quote),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
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
