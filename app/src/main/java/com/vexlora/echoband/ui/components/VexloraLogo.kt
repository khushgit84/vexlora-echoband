package com.vexlora.echoband.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexlora.echoband.ui.theme.ElectricBlue
import com.vexlora.echoband.ui.theme.TextPrimary
import com.vexlora.echoband.ui.theme.VioletAccent

@Composable
fun VexloraLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "logo_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top glowing accent line
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(3.dp)
                .alpha(glowAlpha)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(ElectricBlue, VioletAccent)))
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "VEXLORA",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 7.sp,
                fontSize = 28.sp
            ),
            color = TextPrimary
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = "ECHOBAND",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 8.sp,
                fontSize = 13.sp
            ),
            color = ElectricBlue
        )

        Spacer(Modifier.height(10.dp))

        // Bottom glowing accent line
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(3.dp)
                .alpha(glowAlpha)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(VioletAccent, ElectricBlue)))
        )
    }
}
