package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
import com.example.model.VpnConfig
import com.example.model.VpnStatus
import com.example.ui.MainViewModel
import com.example.ui.components.ConnectButton
import com.example.ui.components.MetricsGrid
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToConfigs: () -> Unit,
    onNavigateToLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnStatus by viewModel.vpnStatus.collectAsState()
    val stats by viewModel.trafficStats.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val configs by viewModel.configs.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var showServerSelectorSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val isConnected = vpnStatus == VpnStatus.CONNECTED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header Bar
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(1.dp, CyanPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Logo",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DARK TUNNEL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TextPrimary,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "SECURE NETWORK PROTOCOL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Status pill
                val (statusDotColor, statusLabel) = when (vpnStatus) {
                    VpnStatus.CONNECTED -> NeonGreen to "PROTECTED"
                    VpnStatus.CONNECTING -> CyanPrimary to "CONNECTING"
                    VpnStatus.RECONNECTING -> AmberWarning to "RETRYING"
                    VpnStatus.ERROR -> CrimsonError to "ERROR"
                    VpnStatus.DISCONNECTED -> TextMuted to "UNPROTECTED"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, statusDotColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = "Status dot",
                            tint = statusDotColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusDotColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Selected Server / Tunnel Card
        item {
            val cfg = activeConfig ?: configs.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .clickable { showServerSelectorSheet = true }
                    .padding(14.dp)
                    .testTag("active_server_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Server",
                                tint = ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = cfg?.name ?: "No Profile Selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyanPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cfg?.protocol?.displayName ?: "Select Server",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CyanPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${cfg?.serverHost ?: ""}:${cfg?.serverPort ?: ""}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select Server",
                        tint = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Center: Interactive Big Connect Button
        item {
            ConnectButton(
                status = vpnStatus,
                onClick = {
                    if (context is com.example.MainActivity) {
                        (context as com.example.MainActivity).requestVpnAndConnect()
                    } else {
                        viewModel.toggleVpn(context)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Real-time Metrics Grid
        item {
            MetricsGrid(
                stats = stats,
                isConnected = isConnected
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Logs Console Snippet
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .clickable { onNavigateToLogs() }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Logs",
                                tint = CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE TUNNEL LOGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "View Terminal >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val recentLogs = logs.takeLast(3)
                    if (recentLogs.isEmpty()) {
                        Text(
                            text = "Engine ready. Tap Connect to establish encrypted tunnel.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    } else {
                        recentLogs.forEach { entry ->
                            val color = when (entry.level) {
                                LogLevel.ERROR -> CrimsonError
                                LogLevel.WARN -> AmberWarning
                                LogLevel.SUCCESS -> NeonGreen
                                LogLevel.DEBUG -> TextMuted
                                LogLevel.INFO -> CyanPrimary
                            }
                            Text(
                                text = "> ${entry.message}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = color,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Sheet for Quick Server Selection
    if (showServerSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showServerSelectorSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Server Node",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    ElevatedButton(
                        onClick = {
                            showServerSelectorSheet = false
                            onNavigateToConfigs()
                        },
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Text("Manage", fontSize = 12.sp, color = CyanPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                configs.forEach { item ->
                    val isSelected = (activeConfig?.id ?: "") == item.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyanPrimary.copy(alpha = 0.12f) else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) CyanPrimary else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.selectConfig(item)
                                showServerSelectorSheet = false
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyanPrimary else TextPrimary
                                )
                                Text(
                                    text = "${item.protocol.displayName} • ${item.serverHost}:${item.serverPort}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
