package com.vexlora.echoband.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vexlora.echoband.model.AssistantState
import com.vexlora.echoband.ui.theme.ElectricBlue
import com.vexlora.echoband.ui.theme.StatusError
import com.vexlora.echoband.ui.theme.StatusListening
import com.vexlora.echoband.ui.theme.StatusSpeaking
import com.vexlora.echoband.ui.theme.StatusThinking
import com.vexlora.echoband.ui.theme.VioletAccent

@Composable
fun MicrophoneButton(
    assistantState: AssistantState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_animation")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_1"
    )

    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale_2"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (assistantState) {
            AssistantState.IDLE -> ElectricBlue
            AssistantState.LISTENING -> StatusListening
            AssistantState.THINKING -> VioletAccent
            AssistantState.SPEAKING -> StatusSpeaking
            AssistantState.ERROR -> StatusError
        },
        animationSpec = tween(400),
        label = "btn_color"
    )

    val isPulsing = assistantState == AssistantState.LISTENING ||
            assistantState == AssistantState.SPEAKING ||
            assistantState == AssistantState.THINKING

    val icon = when (assistantState) {
        AssistantState.IDLE -> Icons.Default.Mic
        AssistantState.LISTENING -> Icons.Default.Stop
        AssistantState.THINKING -> Icons.Default.Mic
        AssistantState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
        AssistantState.ERROR -> Icons.Default.MicOff
    }

    val isClickable = assistantState == AssistantState.IDLE ||
            assistantState == AssistantState.LISTENING ||
            assistantState == AssistantState.ERROR

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer animated radar pulse ring (when active)
        if (isPulsing) {
            Box(
                Modifier
                    .size(150.dp)
                    .scale(if (assistantState == AssistantState.LISTENING) pulseScale1 else pulseScale2)
                    .alpha(if (assistantState == AssistantState.LISTENING) pulseAlpha1 else 0.25f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(buttonColor.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )
        }

        // Mid halo ring
        Box(
            Modifier
                .size(120.dp)
                .scale(if (assistantState == AssistantState.LISTENING) pulseScale2 else 1f)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(buttonColor.copy(alpha = 0.6f), buttonColor.copy(alpha = 0.15f))
                    ),
                    shape = CircleShape
                )
        )

        // Core action button
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(buttonColor.copy(alpha = 0.35f), buttonColor.copy(alpha = 0.12f))
                    )
                )
                .border(2.5.dp, buttonColor.copy(alpha = 0.85f), CircleShape)
                .clickable(
                    enabled = isClickable,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Microphone Action",
                tint = buttonColor,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}
