package com.vexlora.echoband.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vexlora.echoband.model.BluetoothState
import com.vexlora.echoband.ui.theme.ElectricBlue
import com.vexlora.echoband.ui.theme.StatusError
import com.vexlora.echoband.ui.theme.StatusSpeaking
import com.vexlora.echoband.ui.theme.TextPrimary
import com.vexlora.echoband.ui.theme.TextSecondary
import com.vexlora.echoband.ui.theme.TextTertiary
import com.vexlora.echoband.ui.theme.VexloraCardBackground

@Composable
fun BluetoothStatusCard(
    state: BluetoothState,
    deviceName: String?,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (state) {
            BluetoothState.CONNECTED -> StatusSpeaking
            BluetoothState.SEARCHING -> ElectricBlue
            BluetoothState.DISCONNECTED -> StatusError
        },
        animationSpec = tween(400),
        label = "bt_status_color"
    )

    val icon = when (state) {
        BluetoothState.CONNECTED -> Icons.Default.Headphones
        BluetoothState.SEARCHING -> Icons.Default.BluetoothConnected
        BluetoothState.DISCONNECTED -> Icons.Default.BluetoothDisabled
    }

    val statusLabel = when (state) {
        BluetoothState.CONNECTED -> "Sony WH-CH510 Active"
        BluetoothState.SEARCHING -> "Searching for Headphones…"
        BluetoothState.DISCONNECTED -> "Headphones Disconnected"
    }

    val subtitleLabel = when (state) {
        BluetoothState.CONNECTED -> deviceName ?: "Sony WH-CH510 Connected"
        BluetoothState.SEARCHING -> "Scanning for paired audio devices"
        BluetoothState.DISCONNECTED -> "Pair Sony WH-CH510 via Bluetooth settings"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VexloraCardBackground)
            .border(
                width = 1.dp,
                color = statusColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Icon container with tinted circular background
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Bluetooth Status",
                tint = statusColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = subtitleLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
