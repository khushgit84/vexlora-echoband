package com.vexlora.echoband.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vexlora.echoband.model.AssistantState
import com.vexlora.echoband.ui.theme.StatusError
import com.vexlora.echoband.ui.theme.StatusIdle
import com.vexlora.echoband.ui.theme.StatusListening
import com.vexlora.echoband.ui.theme.StatusSpeaking
import com.vexlora.echoband.ui.theme.StatusThinking
import com.vexlora.echoband.ui.theme.VexloraCardBackground

@Composable
fun StatusIndicator(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val color by animateColorAsState(
        targetValue = when (state) {
            AssistantState.IDLE -> StatusIdle
            AssistantState.LISTENING -> StatusListening
            AssistantState.THINKING -> StatusThinking
            AssistantState.SPEAKING -> StatusSpeaking
            AssistantState.ERROR -> StatusError
        },
        animationSpec = tween(350),
        label = "status_color"
    )

    val label = when (state) {
        AssistantState.IDLE -> "Ready • Tap mic to speak"
        AssistantState.LISTENING -> "Listening to audio…"
        AssistantState.THINKING -> "Processing with Gemini…"
        AssistantState.SPEAKING -> "EchoBand speaking…"
        AssistantState.ERROR -> "Attention required"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(VexloraCardBackground)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(9.dp)
                .alpha(if (state != AssistantState.IDLE) dotAlpha else 0.8f)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}
