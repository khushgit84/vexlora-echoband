package com.vexlora.echoband.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.vexlora.echoband.model.ConversationMessage
import com.vexlora.echoband.ui.theme.ElectricBlue
import com.vexlora.echoband.ui.theme.TextPrimary
import com.vexlora.echoband.ui.theme.VioletAccent

@Composable
fun ConversationBubble(
    message: ConversationMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val bubbleBackground = if (isUser) {
        Brush.linearGradient(
            listOf(
                ElectricBlue.copy(alpha = 0.20f),
                ElectricBlue.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                VioletAccent.copy(alpha = 0.22f),
                VioletAccent.copy(alpha = 0.10f)
            )
        )
    }

    val borderColor = if (isUser) ElectricBlue.copy(alpha = 0.35f) else VioletAccent.copy(alpha = 0.35f)
    val accentColor = if (isUser) ElectricBlue else VioletAccent
    val icon = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome
    val roleTitle = if (isUser) "You" else "Gemini EchoBand"

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(bubbleShape)
                .background(bubbleBackground)
                .border(1.dp, borderColor, bubbleShape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = roleTitle,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = roleTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}
