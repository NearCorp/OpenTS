package dev.tsdroid.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tsdroid.R
import dev.tsdroid.ui.component.AppSettingsDialog
import dev.tsdroid.ui.component.ChannelTree
import dev.tsdroid.viewmodel.ConnectionViewModel
import dev.tslib.ConnectionState
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val serverAvatarColors = listOf(
    Color(0xFF5C6BC0), // indigo
    Color(0xFF26A69A), // teal
    Color(0xFFEF5350), // red
    Color(0xFFAB47BC), // purple
    Color(0xFF42A5F5), // blue
    Color(0xFFFF7043), // deep orange
    Color(0xFF66BB6A), // green
    Color(0xFFEC407A), // pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    onConnected: () -> Unit,
    viewModel: ConnectionViewModel = viewModel(),
) {
    val address by viewModel.address.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val password by viewModel.password.collectAsState()
    val channel by viewModel.channel.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkIcons by viewModel.bookmarkIcons.collectAsState()
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val editingIndex by viewModel.editingIndex.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val error by viewModel.error.collectAsState()
    val browsedChannels by viewModel.browsedChannels.collectAsState()
    val isBrowsing by viewModel.isBrowsing.collectAsState()
    val showChannelPicker by viewModel.showChannelPicker.collectAsState()

    // Settings
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isPttMode by viewModel.isPttMode.collectAsState()
    val vadThreshold by viewModel.vadThreshold.collectAsState()
    val noiseSuppressionEnabled by viewModel.noiseSuppressionEnabled.collectAsState()
    val echoCancellationEnabled by viewModel.echoCancellationEnabled.collectAsState()
    val audioSource by viewModel.audioSource.collectAsState()
    val showLinkThumbnails by viewModel.showLinkThumbnails.collectAsState()
    val autoLoadImages by viewModel.autoLoadImages.collectAsState()
    val audioGain by viewModel.audioGain.collectAsState()
    val speakerphoneOn by viewModel.speakerphoneOn.collectAsState()
    val customBackgroundPath by viewModel.customBackgroundPath.collectAsState()
    val backgroundDim by viewModel.backgroundDim.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val bookmarkSavedMsg = stringResource(R.string.bookmark_saved)
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val isConnecting = connectionState == ConnectionState.CONNECTING

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* checked at usage */ }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(autoReconnect) {
        if (autoReconnect) {
            viewModel.tryAutoReconnect(onConnected)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Headset,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.welcome_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.welcome_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (customBackgroundPath.isNotBlank()) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.manual_connection))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (customBackgroundPath.isNotBlank()) {
                val bgFile = remember(customBackgroundPath) { File(customBackgroundPath) }
                if (bgFile.exists()) {
                    AsyncImage(
                        model = bgFile,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundDim)),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            Spacer(Modifier.height(4.dp))

            // Quick Connect Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.quick_connect),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { viewModel.address.value = it },
                        label = { Text(stringResource(R.string.server_address)) },
                        placeholder = { Text(stringResource(R.string.server_address_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConnecting,
                    )

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { viewModel.nickname.value = it },
                        label = { Text(stringResource(R.string.nickname)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConnecting,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.connect(onConnected) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isConnecting,
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(stringResource(if (isConnecting) R.string.connecting else R.string.connect_now))
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.saveBookmark()
                                scope.launch { snackbarHostState.showSnackbar(bookmarkSavedMsg) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = address.isNotBlank() && nickname.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Bookmarks Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.server_bookmarks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (bookmarks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "${bookmarks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (bookmarks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_bookmarks),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                bookmarks.forEachIndexed { index, bookmark ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val icon = if (bookmark.iconId != 0L) bookmarkIcons[bookmark.iconId] else null
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                val sName = bookmark.serverName ?: bookmark.name
                                val color = serverAvatarColors[(sName.hashCode() and 0x7FFFFFFF) % serverAvatarColors.size]
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = sName.firstOrNull()?.uppercase() ?: "S",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.serverName ?: bookmark.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${bookmark.nickname} \u00B7 ${bookmark.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FilledTonalButton(
                                onClick = { viewModel.connectBookmark(bookmark, onConnected) },
                                enabled = !isConnecting,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.connect))
                            }
                            Box {
                                var menuExpanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = null,
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit)) },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.editBookmark(bookmark, index)
                                            showBottomSheet = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.remove)) },
                                        onClick = {
                                            menuExpanded = false
                                            deleteConfirmIndex = index
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
        }

        // Settings Dialog on Main Screen
        if (showSettings) {
            AppSettingsDialog(
                appLanguage = appLanguage,
                onAppLanguageChange = { viewModel.setAppLanguage(it) },
                isPttMode = isPttMode,
                onPttModeChange = { viewModel.setIsPttMode(it) },
                vadThreshold = vadThreshold,
                onVadThresholdChange = { viewModel.setVadThreshold(it) },
                speakerphoneOn = speakerphoneOn,
                onSpeakerphoneOnChange = { viewModel.setSpeakerphoneOn(it) },
                noiseSuppressionEnabled = noiseSuppressionEnabled,
                onNoiseSuppressionChange = { viewModel.setNoiseSuppressionEnabled(it) },
                echoCancellationEnabled = echoCancellationEnabled,
                onEchoCancellationChange = { viewModel.setEchoCancellationEnabled(it) },
                audioSource = audioSource,
                onAudioSourceChange = { viewModel.setAudioSource(it) },
                currentGain = audioGain,
                onGainChange = { viewModel.setAudioGain(it) },
                customBackgroundPath = customBackgroundPath,
                backgroundDim = backgroundDim,
                onSelectBackground = { viewModel.setCustomBackground(it) },
                onClearBackground = { viewModel.clearCustomBackground() },
                onBackgroundDimChange = { viewModel.setBackgroundDim(it) },
                showLinkThumbnails = showLinkThumbnails,
                onShowLinkThumbnailsChange = { viewModel.setShowLinkThumbnails(it) },
                autoLoadImages = autoLoadImages,
                onAutoLoadImagesChange = { viewModel.setAutoLoadImages(it) },
                autoReconnect = autoReconnect,
                onAutoReconnectChange = { viewModel.setAutoReconnect(it) },
                onDismiss = { showSettings = false },
            )
        }

        // Delete confirmation dialog
        deleteConfirmIndex?.let { idx ->
            val bookmarkName = bookmarks.getOrNull(idx)?.let { it.serverName ?: it.name } ?: ""
            AlertDialog(
                onDismissRequest = { deleteConfirmIndex = null },
                title = { Text(stringResource(R.string.remove)) },
                text = { Text(stringResource(R.string.confirm_delete, bookmarkName)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeBookmark(idx)
                        deleteConfirmIndex = null
                    }) {
                        Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmIndex = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        // Channel picker dialog
        if (showChannelPicker) {
            Dialog(onDismissRequest = { viewModel.showChannelPicker.value = false }) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            stringResource(R.string.select_channel),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(16.dp))
                        ChannelTree(
                            channels = browsedChannels,
                            users = emptyList(),
                            onChannelClick = { viewModel.selectChannel(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(
                            onClick = { viewModel.showChannelPicker.value = false },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }

        // Connection / Add Bookmark Bottom Sheet
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    viewModel.cancelEdit()
                },
                sheetState = sheetState,
            ) {
                val isEditing = editingIndex >= 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(if (isEditing) R.string.edit_bookmark else R.string.manual_connection),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { viewModel.address.value = it },
                        label = { Text(stringResource(R.string.server_address)) },
                        placeholder = { Text(stringResource(R.string.server_address_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConnecting,
                    )

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { viewModel.nickname.value = it },
                        label = { Text(stringResource(R.string.nickname)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConnecting,
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.password.value = it },
                        label = { Text(stringResource(R.string.password_optional)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConnecting,
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    OutlinedTextField(
                        value = channel,
                        onValueChange = { viewModel.channel.value = it },
                        label = { Text(stringResource(R.string.channel_optional)) },
                        placeholder = { Text(stringResource(R.string.default_channel)) },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConnecting,
                        trailingIcon = {
                            if (isBrowsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                IconButton(onClick = { viewModel.browseChannels() }, enabled = !isConnecting) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.browse_channels))
                                }
                            }
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                }
                                viewModel.connect(onConnected)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isConnecting,
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .width(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(stringResource(if (isConnecting) R.string.connecting else R.string.connect))
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.saveBookmark()
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(if (isEditing) R.string.save else R.string.add_bookmark))
                        }
                    }
                }
            }
        }
    }
}
