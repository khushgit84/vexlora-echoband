package com.vexlora.echoband.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vexlora.echoband.model.AssistantState
import com.vexlora.echoband.ui.components.BluetoothStatusCard
import com.vexlora.echoband.ui.components.ConversationBubble
import com.vexlora.echoband.ui.components.MicrophoneButton
import com.vexlora.echoband.ui.components.StatusIndicator
import com.vexlora.echoband.ui.components.VexloraLogo
import com.vexlora.echoband.ui.theme.ElectricBlue
import com.vexlora.echoband.ui.theme.StatusError
import com.vexlora.echoband.ui.theme.StatusSpeaking
import com.vexlora.echoband.ui.theme.StatusThinking
import com.vexlora.echoband.ui.theme.TextPrimary
import com.vexlora.echoband.ui.theme.TextSecondary
import com.vexlora.echoband.ui.theme.TextTertiary
import com.vexlora.echoband.ui.theme.VexloraCardBackground
import com.vexlora.echoband.ui.theme.VexloraDeepBlack
import com.vexlora.echoband.ui.theme.VioletAccent
import com.vexlora.echoband.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val assistantState by viewModel.assistantState.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val conversation by viewModel.conversationHistory.collectAsState()
    val btState by viewModel.bluetoothState.collectAsState()
    val btName by viewModel.bluetoothDeviceName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    val isAutoListen by viewModel.isAutoListen.collectAsState()
    val isBackgroundMode by viewModel.isBackgroundMode.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val volumePercent by viewModel.volumePercent.collectAsState()
    val rememberedFacts by viewModel.rememberedFacts.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editedApiKey by remember(apiKey) { mutableStateOf(apiKey) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasMicPermission = grants[Manifest.permission.RECORD_AUDIO] == true
        if (hasMicPermission) {
            viewModel.startListening()
        }
    }

    val listState = rememberLazyListState()

    // Smooth scroll to the latest conversation message
    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            delay(120)
            val count = listState.layoutInfo.totalItemsCount
            if (count > 0) {
                listState.animateScrollToItem(count - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VexloraDeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Navigation Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.refreshBluetoothStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Bluetooth",
                            tint = TextTertiary
                        )
                    }

                    // Continuous Auto-Listening Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isAutoListen) ElectricBlue.copy(alpha = 0.15f) else VexloraCardBackground)
                            .border(
                                1.dp,
                                if (isAutoListen) ElectricBlue.copy(alpha = 0.5f) else TextTertiary.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.toggleAutoListen() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Auto Listen",
                                tint = if (isAutoListen) ElectricBlue else TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isAutoListen) "Auto-Listen ON" else "Auto-Listen OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAutoListen) ElectricBlue else TextTertiary
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    if (conversation.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearConversation() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = TextTertiary
                            )
                        }
                    }
                }

                // Settings button
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = ElectricBlue
                    )
                }
            }

            // ── Scrollable Body ──
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Logo and Branding
                item(key = "logo_section") {
                    Spacer(Modifier.height(4.dp))
                    VexloraLogo()
                    Spacer(Modifier.height(16.dp))
                }

                // 2. Bluetooth Connection Status (Sony WH-CH510)
                item(key = "bluetooth_status") {
                    BluetoothStatusCard(
                        state = btState,
                        deviceName = btName
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // 2.1 Hardware Quick Controls Strip
                item(key = "device_controls_strip") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VexloraCardBackground)
                            .border(1.dp, VioletAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Torch Quick Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleTorch() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Torch",
                                tint = if (isTorchOn) ElectricBlue else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isTorchOn) "Torch ON" else "Torch OFF",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isTorchOn) ElectricBlue else TextSecondary
                            )
                        }

                        // Volume Quick Stepper
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.decreaseVolume() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                                    contentDescription = "Volume Down",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "$volumePercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = { viewModel.increaseVolume() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Volume Up",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // 3. Microphone Action Button
                item(key = "microphone_button") {
                    MicrophoneButton(
                        assistantState = assistantState,
                        onClick = {
                            when (assistantState) {
                                AssistantState.IDLE, AssistantState.ERROR -> {
                                    if (hasMicPermission) {
                                        viewModel.startListening()
                                    } else {
                                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                                        }
                                        permissionLauncher.launch(perms.toTypedArray())
                                    }
                                }
                                AssistantState.LISTENING -> {
                                    viewModel.stopListening()
                                }
                                AssistantState.THINKING, AssistantState.SPEAKING -> {
                                    viewModel.startListening()
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // 4. Status Indicator (Listening / Thinking / Speaking)
                item(key = "status_indicator") {
                    StatusIndicator(state = assistantState)
                    Spacer(Modifier.height(18.dp))
                }

                // Loading spinner during THINKING state
                if (assistantState == AssistantState.THINKING) {
                    item(key = "thinking_progress") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = StatusThinking,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Thinking with Gemini (Continuous Memory)…",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusThinking
                            )
                        }
                    }
                }

                // Error State Banner
                item(key = "error_banner") {
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMessage?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(StatusError.copy(alpha = 0.12f))
                                    .border(1.dp, StatusError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusError,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissError() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss error",
                                            tint = StatusError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. User's recognized speech card
                item(key = "recognized_speech") {
                    AnimatedVisibility(
                        visible = recognizedText.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            ElectricBlue.copy(alpha = 0.14f),
                                            ElectricBlue.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .border(1.dp, ElectricBlue.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Recognized",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "User Recognized Speech",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricBlue
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = recognizedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // 6. Gemini's response card
                item(key = "gemini_response") {
                    AnimatedVisibility(
                        visible = aiResponse.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            VioletAccent.copy(alpha = 0.16f),
                                            VioletAccent.copy(alpha = 0.06f)
                                        )
                                    )
                                )
                                .border(1.dp, VioletAccent.copy(alpha = 0.40f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini AI",
                                        tint = VioletAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "EchoBand with Continuous Memory",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VioletAccent
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = aiResponse,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Voice Command Suggestion Pills
                item(key = "voice_suggestions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "VOICE CONTROLS ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextTertiary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "• \"Turn on / off torch\"\n• \"Volume up / down / to 80%\"\n• \"Remember my car is at B2\"\n• \"Set timer for 5 minutes\"\n• \"Set alarm for 7:30 AM\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // 7. Voice Conversation History with Continuous Memory
                if (conversation.isNotEmpty()) {
                    item(key = "conversation_header") {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CONVERSATION MEMORY (${conversation.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextTertiary
                            )

                            Text(
                                text = if (isAutoListen) "Auto-Listen Active" else "Push to Talk",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAutoListen) ElectricBlue else TextTertiary
                            )
                        }
                    }

                    items(items = conversation, key = { it.id }) { message ->
                        ConversationBubble(message = message)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item(key = "footer_space") {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        // 8. Settings & Direct Controls Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                containerColor = VexloraCardBackground,
                shape = RoundedCornerShape(22.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "EchoBand Controls",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        // Section: Direct Gemini API Key
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "API Key",
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Gemini API Key",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Paste directly into EchoBand. Remembered securely on device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editedApiKey,
                                onValueChange = {
                                    editedApiKey = it
                                    viewModel.updateApiKey(it)
                                },
                                label = { Text("Gemini API Key") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val clipText = clipboardManager.getText()?.text.orEmpty().trim()
                                        if (clipText.isNotBlank()) {
                                            editedApiKey = clipText
                                            viewModel.updateApiKey(clipText)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = ElectricBlue
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = TextTertiary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = ElectricBlue,
                                    unfocusedLabelColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(14.dp))
                        }

                        // Section: Background Service Mode
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Background Assistant",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Keep EchoBand active when phone is locked or in pocket with headphones.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = isBackgroundMode,
                                    onCheckedChange = { viewModel.toggleBackgroundService() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricBlue,
                                        checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        // Section: Auto-Listen Continuous Mode
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Continuous Auto-Listen",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Automatically listen after speaking for hands-free conversations.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = isAutoListen,
                                    onCheckedChange = { viewModel.toggleAutoListen() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricBlue,
                                        checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        // Section: Remembered Things Store
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "Remembered Facts",
                                    tint = VioletAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Remembered Things (${rememberedFacts.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Say 'Remember that...' and EchoBand stores it permanently.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(Modifier.height(6.dp))

                            if (rememberedFacts.isEmpty()) {
                                Text(
                                    text = "No stored facts yet. Try saying: \"Remember my key is in the bag\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            } else {
                                rememberedFacts.forEach { fact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(VexloraDeepBlack)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = fact.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteFact(fact.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = StatusError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                        }

                        // Section: Headphone Setup (Sony WH-CH510)
                        item {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricBlue,
                                    contentColor = VexloraDeepBlack
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Bluetooth Settings")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateApiKey(editedApiKey)
                        showSettingsDialog = false
                    }) {
                        Text("Save & Close", color = ElectricBlue)
                    }
                }
            )
        }
    }
}
