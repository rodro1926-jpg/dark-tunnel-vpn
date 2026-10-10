package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrafficStats
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MetricsGrid(
    stats: TrafficStats,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Download & Upload Speeds
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                icon = Icons.Default.ArrowDownward,
                iconColor = NeonGreen,
                label = "DOWNLOAD",
                primaryValue = if (isConnected) stats.formattedDownSpeed() else "0 B/s",
                secondaryValue = "Total: ${stats.formattedDownBytes()}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.ArrowUpward,
                iconColor = CyanPrimary,
                label = "UPLOAD",
                primaryValue = if (isConnected) stats.formattedUpSpeed() else "0 B/s",
                secondaryValue = "Total: ${stats.formattedUpBytes()}",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Duration & Ping
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                icon = Icons.Default.Timer,
                iconColor = ElectricBlue,
                label = "DURATION",
                primaryValue = if (isConnected) stats.formattedDuration() else "00:00:00",
                secondaryValue = if (isConnected) "Session Live" else "Idle",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Speed,
                iconColor = if (stats.pingMs in 0..120) NeonGreen else if (stats.pingMs > 250) Color(0xFFFF9800) else ElectricBlue,
                label = "PING",
                primaryValue = if (isConnected && stats.pingMs >= 0) "${stats.pingMs} ms" else if (isConnected) "Pinging..." else "- -",
                secondaryValue = "Tunnel Latency",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Public IP Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Public IP",
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PUBLIC IP ADDRESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (isConnected) stats.publicIp else "Encrypted upon connection",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    primaryValue: String,
    secondaryValue: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = primaryValue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondaryValue,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
