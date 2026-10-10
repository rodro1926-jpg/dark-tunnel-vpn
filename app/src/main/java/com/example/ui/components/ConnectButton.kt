package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VpnStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen

@Composable
fun ConnectButton(
    status: VpnStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == VpnStatus.CONNECTING || status == VpnStatus.RECONNECTING) 1.25f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val (primaryColor, glowColor, statusText) = when (status) {
        VpnStatus.CONNECTED -> Triple(NeonGreen, Color(0x4400E676), "CONNECTED")
        VpnStatus.CONNECTING -> Triple(CyanPrimary, Color(0x4400E5FF), "CONNECTING...")
        VpnStatus.RECONNECTING -> Triple(AmberWarning, Color(0x44FFB300), "RECONNECTING...")
        VpnStatus.ERROR -> Triple(CrimsonError, Color(0x44FF1744), "TAP TO RETRY")
        VpnStatus.DISCONNECTED -> Triple(ElectricBlue, Color(0x332979FF), "TAP TO CONNECT")
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(240.dp)
    ) {
        // Outer glowing pulse ring
        if (status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING || status == VpnStatus.RECONNECTING) {
            Canvas(modifier = Modifier.size(230.dp * pulseScale)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = center,
                        radius = size.width / 2
                    )
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = size.width / 2.2f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Circular background button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(175.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = primaryColor, spotColor = primaryColor)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A2638), Color(0xFF0D1522))
                    )
                )
                .border(3.dp, primaryColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = primaryColor),
                    onClick = onClick
                )
                .testTag("vpn_connect_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (status) {
                    VpnStatus.CONNECTING, VpnStatus.RECONNECTING -> {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Syncing",
                            tint = primaryColor,
                            modifier = Modifier
                                .size(56.dp)
                                .rotate(rotationAngle)
                        )
                    }
                    VpnStatus.CONNECTED -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Connected",
                            tint = primaryColor,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Connect",
                            tint = primaryColor,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusText,
                    color = primaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
