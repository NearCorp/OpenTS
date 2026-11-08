package dev.tsdroid.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dev.tsdroid.bridge.InputDeviceInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tsdroid.R
import dev.tsdroid.ui.component.AppSettingsDialog
import dev.tsdroid.ui.component.ChannelTree
import dev.tsdroid.ui.component.ChatView
import dev.tsdroid.ui.component.FileManagerDialog
import dev.tsdroid.ui.component.ScreenShareViewerDialog
import dev.tsdroid.ui.component.ShareTarget
import dev.tsdroid.viewmodel.ChatMessage
import dev.tsdroid.viewmodel.DownloadState
import dev.tsdroid.viewmodel.FileAttachment
import dev.tsdroid.viewmodel.ServerViewModel
import dev.tslib.ConnectionState
import dev.tslib.User
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    onDisconnected: () -> Unit,
    viewModel: ServerViewModel = viewModel(),
) {
    val channels by viewModel.channels.collectAsState()
    val users by viewModel.users.collectAsState()
    val channelIcons by viewModel.channelIcons.collectAsState()
    val userAvatars by viewModel.avatars.collectAsState()
    val serverInfo by viewModel.serverInfo.collectAsState()
    val channelMessages by viewModel.channelMessages.collectAsState()
    val privateMessages by viewModel.privateMessages.collectAsState()
    val isPttMode by viewModel.isPttMode.collectAsState()
    val vadThreshold by viewModel.vadThreshold.collectAsState()
    val noiseSuppressionEnabled by viewModel.noiseSuppressionEnabled.collectAsState()
    val echoCancellationEnabled by viewModel.echoCancellationEnabled.collectAsState()
    val micLevel by viewModel.micLevel.collectAsState()
    val isVoiceActive by viewModel.isVoiceActive.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val audioSource by viewModel.audioSource.collectAsState()
    val preferredInputDeviceId by viewModel.preferredInputDeviceId.collectAsState()
    val availableInputDevices by viewModel.availableInputDevices.collectAsState()
    val userVolumes by viewModel.userVolumes.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val unreadChannel by viewModel.unreadChannel.collectAsState()
    val unreadPrivate by viewModel.unreadPrivate.collectAsState()
    val audioGain by viewModel.audioGain.collectAsState()
    val showLinkThumbnails by viewModel.showLinkThumbnails.collectAsState()
    val autoLoadImages by viewModel.autoLoadImages.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val fileManagerOpen by viewModel.fileManagerOpen.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    val currentFilePath by viewModel.currentFilePath.collectAsState()
    val fileManagerLoading by viewModel.fileManagerLoading.collectAsState()
    val channelPermissions by viewModel.currentChannelPermissions.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var selectedUserForDetails by remember { mutableStateOf<User?>(null) }
    var userForScreenShare by remember { mutableStateOf<User?>(null) }
    var chatOpen by remember { mutableStateOf(false) }
    var chatEverOpened by remember { mutableStateOf(false) }
    var chatTab by remember { mutableIntStateOf(0) }
    var messageText by remember { mutableStateOf("") }
    var pmTargetId by remember { mutableStateOf<Int?>(null) }

    val pmTarget = pmTargetId?.let { id -> users.find { it.id == id } }

    val streamUrlFromChat = remember(channelMessages) {
        val streamRegex = Regex("""(?:https?://|rtsp://|rtmp://)[^\s]+""", RegexOption.IGNORE_CASE)
        channelMessages.lastOrNull { msg ->
            streamRegex.containsMatchIn(msg.text)
        }?.let { msg ->
            streamRegex.find(msg.text)?.value to msg.senderId
        }
    }

    val streamingUsers = remember(users, streamUrlFromChat) {
        users.filter { user ->
            user.description?.contains("stream", ignoreCase = true) == true ||
            user.description?.contains("http", ignoreCase = true) == true ||
            user.awayMessage?.contains("stream", ignoreCase = true) == true ||
            (streamUrlFromChat != null && user.id == streamUrlFromChat.second)
        }
    }

    val context = LocalContext.current
    val pmConversationUsers = remember(privateMessages, users) {
        privateMessages.keys.map { userId ->
            val name = users.find { it.id == userId }?.nickname
                ?: privateMessages[userId]?.lastOrNull { !it.isMe }?.sender
                ?: context.getString(R.string.user_fallback, userId)
            userId to name
        }
    }

    val totalUnreadPrivate = unreadPrivate.values.sum()

    LaunchedEffect(chatOpen, chatTab, pmTargetId) {
        viewModel.setChatState(chatOpen, chatTab, pmTargetId)
    }

    DisposableEffect(Unit) {
        viewModel.bindToService()
        onDispose {}
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            onDisconnected()
        }
    }
    if (connectionState == ConnectionState.DISCONNECTED) return

    val totalUnread = unreadChannel + totalUnreadPrivate

    if (showSettings) {
        AppSettingsDialog(
            appLanguage = appLanguage,
            onAppLanguageChange = { viewModel.setAppLanguage(it) },
            isPttMode = isPttMode,
            onPttModeChange = { viewModel.setIsPttMode(it) },
            vadThreshold = vadThreshold,
            onVadThresholdChange = { viewModel.setVadThreshold(it) },
            micLevel = micLevel,
            noiseSuppressionEnabled = noiseSuppressionEnabled,
            onNoiseSuppressionChange = { viewModel.setNoiseSuppressionEnabled(it) },
            echoCancellationEnabled = echoCancellationEnabled,
            onEchoCancellationChange = { viewModel.setEchoCancellationEnabled(it) },
            audioSource = audioSource,
            onAudioSourceChange = { viewModel.setAudioSource(it) },
            preferredDeviceId = preferredInputDeviceId,
            onPreferredDeviceIdChange = { viewModel.setPreferredInputDeviceId(it) },
            availableDevices = availableInputDevices,
            currentGain = audioGain,
            onGainChange = { viewModel.setAudioGain(it) },
            showLinkThumbnails = showLinkThumbnails,
            onShowLinkThumbnailsChange = { viewModel.setShowLinkThumbnails(it) },
            autoLoadImages = autoLoadImages,
            onAutoLoadImagesChange = { viewModel.setAutoLoadImages(it) },
            autoReconnect = autoReconnect,
            onAutoReconnectChange = { viewModel.setAutoReconnect(it) },
            onDismiss = { showSettings = false },
        )
    }

    userForScreenShare?.let { streamer ->
        val chatUrl = if (streamUrlFromChat?.second == streamer.id) streamUrlFromChat.first else null
        ScreenShareViewerDialog(
            user = streamer,
            avatar = streamer.uid?.let { userAvatars[it] },
            initialStreamUrl = chatUrl,
            onDismiss = { userForScreenShare = null },
        )
    }

    selectedUserForDetails?.let { user ->
        UserDetailBottomSheet(
            user = user,
            avatar = user.uid?.let { userAvatars[it] },
            currentVolume = userVolumes[user.id] ?: 1.0f,
            onVolumeChange = { viewModel.setUserVolume(user.id, it) },
            onSendPm = {
                pmTargetId = user.id
                chatTab = 1
                chatOpen = true
                selectedUserForDetails = null
            },
            onWatchStream = {
                userForScreenShare = user
                selectedUserForDetails = null
            },
            onDismiss = { selectedUserForDetails = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = serverInfo?.name ?: stringResource(R.string.server),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${channels.size} ${stringResource(R.string.tab_channel).lowercase()} \u00B7 ${users.size} online",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val streamer = streamingUsers.firstOrNull() ?: users.firstOrNull()
                        if (streamer != null) {
                            userForScreenShare = streamer
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ScreenShare,
                            contentDescription = stringResource(R.string.screen_share),
                            tint = if (streamingUsers.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFileManager() }) {
                        Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.file_manager))
                    }
                    IconButton(onClick = {
                        viewModel.refreshInputDevices()
                        showSettings = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.disconnect))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: Chat Button with Badge
                    Box {
                        IconButton(
                            onClick = { chatOpen = !chatOpen },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (chatOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Icon(
                                Icons.Default.ChatBubble,
                                contentDescription = stringResource(R.string.chat),
                                tint = if (chatOpen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (totalUnread > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp),
                            ) {
                                Text("$totalUnread")
                            }
                        }
                    }

                    // Center: PTT or VAD Button
                    if (isPttMode) {
                        var isPressed by remember { mutableStateOf(false) }
                        val pttBackground = if (isPressed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer
                        val pttTint = if (isPressed) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(pttBackground)
                                .border(
                                    width = if (isPressed) 4.dp else 0.dp,
                                    color = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            viewModel.setPushToTalk(true)
                                            tryAwaitRelease()
                                            viewModel.setPushToTalk(false)
                                            isPressed = false
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = stringResource(R.string.push_to_talk),
                                    modifier = Modifier.size(30.dp),
                                    tint = pttTint,
                                )
                                Text(
                                    stringResource(R.string.ptt),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = pttTint,
                                )
                            }
                        }
                    } else {
                        // VAD mode:
                        // If muted: shows red mic-off. Tap to unmute.
                        // If unmuted: glows green when speaking. Tap to mute.
                        val vadColor = when {
                            isMuted -> MaterialTheme.colorScheme.errorContainer
                            isVoiceActive -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val vadTint = when {
                            isMuted -> MaterialTheme.colorScheme.onErrorContainer
                            isVoiceActive -> Color.White
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val vadBorderColor = when {
                            isMuted -> MaterialTheme.colorScheme.error
                            isVoiceActive -> Color(0xFF81C784)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(vadColor)
                                .border(
                                    width = if (isVoiceActive || isMuted) 3.dp else 1.dp,
                                    color = vadBorderColor,
                                    shape = CircleShape,
                                )
                                .clickable { viewModel.toggleMute() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = stringResource(if (isMuted) R.string.unmute_mic else R.string.mode_vad),
                                    modifier = Modifier.size(30.dp),
                                    tint = vadTint,
                                )
                                Text(
                                    if (isMuted) stringResource(R.string.status_muted) else "VAD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = vadTint,
                                )
                            }
                        }
                    }

                    // Right: Quick Mode Switch Button (PTT <-> VAD)
                    IconButton(
                        onClick = { viewModel.toggleVoiceMode() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(
                            if (isPttMode) Icons.Default.RecordVoiceOver else Icons.Default.TouchApp,
                            contentDescription = stringResource(R.string.transmission_mode),
                            tint = if (isPttMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (streamingUsers.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { userForScreenShare = streamingUsers.first() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE53935))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.stream_active).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ScreenShare,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${stringResource(R.string.screen_share)}: ${streamingUsers.joinToString(", ") { it.nickname }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(R.string.watch_stream),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                ChannelTree(
                    channels = channels,
                    users = users,
                    onChannelClick = { channelId -> viewModel.moveToChannel(channelId) },
                    onUserClick = { user -> selectedUserForDetails = user },
                    channelIcons = channelIcons,
                    userAvatars = userAvatars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
            }

            // File manager slide-up
            val fileManagerProgress by animateFloatAsState(
                targetValue = if (fileManagerOpen) 0f else 1f,
                animationSpec = tween(300),
                label = "fileManager",
            )
            if (fileManagerOpen || fileManagerProgress < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = size.height * fileManagerProgress },
                ) {
                    FileManagerDialog(
                        currentPath = currentFilePath,
                        files = fileList,
                        isLoading = fileManagerLoading,
                        users = users,
                        permissionHints = channelPermissions,
                        onNavigateToFolder = { viewModel.navigateToFolder(it) },
                        onNavigateUp = { viewModel.navigateUp() },
                        onRefresh = { viewModel.refreshFileList() },
                        onDownload = { viewModel.downloadFileFromManager(it) },
                        onDelete = { viewModel.deleteFileInChannel(it) },
                        onRename = { old, new -> viewModel.renameFileInChannel(old, new) },
                        onCreateDirectory = { viewModel.createDirectoryInChannel(it) },
                        onUploadFile = { name, data -> viewModel.uploadFileToChannel(name, data) },
                        onShareFile = { target, name, size ->
                            when (target) {
                                is ShareTarget.Channel -> viewModel.shareFile(null, name, size)
                                is ShareTarget.PrivateMessage -> viewModel.shareFile(target.userId, name, size)
                            }
                        },
                        onDismiss = { viewModel.closeFileManager() },
                    )
                }
            }

            // Chat slide-up panel
            val chatProgress by animateFloatAsState(
                targetValue = if (chatOpen) 0f else 1f,
                animationSpec = tween(300),
                label = "chatSlide",
            )

            if (chatOpen) chatEverOpened = true

            if (chatEverOpened && (chatOpen || chatProgress < 1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = size.height * chatProgress },
                ) {
                    ChatPanel(
                        chatTab = chatTab,
                        onTabChange = { chatTab = it },
                        unreadChannel = unreadChannel,
                        unreadPrivate = totalUnreadPrivate,
                        pmConversationUsers = pmConversationUsers,
                        pmTargetId = pmTargetId,
                        onPmTargetChange = { pmTargetId = it },
                        unreadPrivateMap = unreadPrivate,
                        channelMessages = channelMessages,
                        privateMessages = privateMessages,
                        showLinkThumbnails = showLinkThumbnails,
                        autoLoadImages = autoLoadImages,
                        onDownloadAttachment = { attachment -> viewModel.downloadAttachment(attachment) },
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSend = {
                            if (chatTab == 0) viewModel.sendChannelMessage(messageText)
                            else pmTarget?.let { viewModel.sendPrivateMessage(it.id, messageText) }
                            messageText = ""
                        },
                        pmTarget = pmTarget,
                        onUploadFile = { fileName, data ->
                            viewModel.uploadAndSendFile(fileName, data, isPrivate = chatTab == 1, targetId = pmTarget?.id)
                        },
                        onClose = { chatOpen = false },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDetailBottomSheet(
    user: User,
    avatar: ImageBitmap?,
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit,
    onSendPm: () -> Unit,
    onWatchStream: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (avatar != null) {
                    Image(
                        bitmap = avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = user.nickname.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = user.nickname,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                if (user.isAway) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.status_away)) },
                        leadingIcon = {
                            Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                if (user.isInputMuted) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.status_muted)) },
                        leadingIcon = {
                            Icon(Icons.Default.MicOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Volume control card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.user_volume),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.user_volume_percent, (currentVolume * 100).toInt()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = currentVolume,
                        onValueChange = onVolumeChange,
                        valueRange = 0.0f..2.0f,
                        steps = 19,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                if (currentVolume == 0f) onVolumeChange(1.0f)
                                else onVolumeChange(0.0f)
                            }
                        ) {
                            Icon(
                                if (currentVolume == 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (currentVolume == 0f) stringResource(R.string.notif_unmute) else stringResource(R.string.mute_user))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onWatchStream,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.watch_stream))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSendPm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.ChatBubble, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.send_private_message))
            }
        }
    }
}



@Composable
private fun ChatPanel(
    chatTab: Int,
    onTabChange: (Int) -> Unit,
    unreadChannel: Int,
    unreadPrivate: Int,
    pmConversationUsers: List<Pair<Int, String>>,
    pmTargetId: Int?,
    onPmTargetChange: (Int) -> Unit,
    unreadPrivateMap: Map<Int, Int>,
    channelMessages: List<ChatMessage>,
    privateMessages: Map<Int, List<ChatMessage>>,
    showLinkThumbnails: Boolean,
    autoLoadImages: Boolean,
    onDownloadAttachment: (FileAttachment) -> StateFlow<DownloadState>,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    pmTarget: User?,
    onUploadFile: (String, ByteArray) -> Unit,
    onClose: () -> Unit,
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Handled via context
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabRow(
                    selectedTabIndex = chatTab,
                    modifier = Modifier.weight(1f),
                ) {
                    Tab(
                        selected = chatTab == 0,
                        onClick = { onTabChange(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.tab_channel))
                                if (unreadChannel > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Badge { Text("$unreadChannel") }
                                }
                            }
                        },
                    )
                    Tab(
                        selected = chatTab == 1,
                        onClick = { onTabChange(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.tab_private))
                                if (unreadPrivate > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Badge { Text("$unreadPrivate") }
                                }
                            }
                        },
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }

            if (chatTab == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for ((userId, name) in pmConversationUsers) {
                        val unread = unreadPrivateMap[userId] ?: 0
                        FilterChip(
                            selected = pmTargetId == userId,
                            onClick = { onPmTargetChange(userId) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name)
                                    if (unread > 0) {
                                        Spacer(Modifier.width(4.dp))
                                        Badge { Text("$unread") }
                                    }
                                }
                            },
                        )
                    }
                }
            }

            val messages = if (chatTab == 0) channelMessages
            else pmTargetId?.let { privateMessages[it] } ?: emptyList()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                ChatView(
                    messages = messages,
                    showLinkThumbnails = showLinkThumbnails,
                    autoLoadImages = autoLoadImages,
                    onDownload = onDownloadAttachment,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (chatTab == 0) stringResource(R.string.message_channel_placeholder)
                            else stringResource(R.string.message_private_placeholder, pmTarget?.nickname ?: "?")
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    enabled = chatTab == 0 || pmTarget != null,
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onSend,
                    enabled = messageText.isNotBlank() && (chatTab == 0 || pmTarget != null),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
