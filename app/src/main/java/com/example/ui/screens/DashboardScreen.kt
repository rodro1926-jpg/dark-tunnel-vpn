package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
import com.example.model.TunnelProtocol
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
    var showQuickServerSetup by remember { mutableStateOf(false) }
    var isPayloadVisible by remember { mutableStateOf(false) }

    var localPayload by remember(activeConfig?.id) { mutableStateOf(activeConfig?.payload ?: "") }
    LaunchedEffect(activeConfig?.payload) {
        localPayload = activeConfig?.payload ?: ""
    }

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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showQuickServerSetup = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Server Endpoint",
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { showServerSelectorSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Select Server",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // SSH Direct / Payload Configuration Card with Show/Hide toggle
        item {
            val cfg = activeConfig ?: configs.firstOrNull()
            val isSsh = cfg?.protocol == TunnelProtocol.SSH_DIRECT ||
                    cfg?.protocol == TunnelProtocol.SSH_SSL ||
                    cfg?.protocol == TunnelProtocol.SSH_HTTP_PAYLOAD

            if (cfg != null && isSsh) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceCard)
                        .border(
                            1.dp,
                            if (isPayloadVisible) CyanPrimary.copy(alpha = 0.5f) else DarkBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Header with Show / Hide Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPayloadVisible = !isPayloadVisible },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPayloadVisible) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Payload",
                                        tint = if (isPayloadVisible) CyanPrimary else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (cfg.protocol == TunnelProtocol.SSH_DIRECT) "SSH Direct Payload Option" else "SSH Payload Injection",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        if (cfg.payload.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(NeonGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isPayloadVisible) "Editor open • Tap to Hide" else if (cfg.payload.isNotBlank()) "Configured (${cfg.payload.length} chars) • Tap to Show" else "Direct raw TCP (No payload) • Tap to Show",
                                        fontSize = 10.sp,
                                        color = if (isPayloadVisible || cfg.payload.isNotBlank()) CyanPrimary else TextMuted
                                    )
                                }
                            }

                            // Show / Hide Pill Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isPayloadVisible) CyanPrimary else DarkSurfaceElevated)
                                    .border(1.dp, if (isPayloadVisible) CyanPrimary else DarkBorder, RoundedCornerShape(20.dp))
                                    .clickable { isPayloadVisible = !isPayloadVisible }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPayloadVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = if (isPayloadVisible) Color(0xFF001E2B) else CyanPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPayloadVisible) "Hide" else "Show",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPayloadVisible) Color(0xFF001E2B) else TextPrimary
                                    )
                                }
                            }
                        }

                        // Expanded Editor Content
                        if (isPayloadVisible) {
                            Text(
                                text = "Quick Templates:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanPrimary.copy(alpha = 0.15f))
                                            .clickable {
                                                localPayload = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Keep-Alive[crlf]User-Agent: [ua][crlf][crlf]"
                                                viewModel.updateActiveConfigPayload(localPayload)
                                                Toast.makeText(context, "Cloudflare WS template loaded & saved", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text("Cloudflare WS", fontSize = 10.sp, color = CyanPrimary)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricBlue.copy(alpha = 0.15f))
                                            .clickable {
                                                localPayload = "CONNECT [host_port] [protocol][crlf]Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]"
                                                viewModel.updateActiveConfigPayload(localPayload)
                                                Toast.makeText(context, "Direct CONNECT template loaded & saved", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text("Direct CONNECT", fontSize = 10.sp, color = ElectricBlue)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonGreen.copy(alpha = 0.15f))
                                            .clickable {
                                                localPayload = "GET http://[host]/ [protocol][crlf]Host: [host][crlf][split]CONNECT [host_port] [protocol][crlf][crlf]"
                                                viewModel.updateActiveConfigPayload(localPayload)
                                                Toast.makeText(context, "Front Split template loaded & saved", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text("Front Split", fontSize = 10.sp, color = NeonGreen)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DarkBorder)
                                            .clickable {
                                                localPayload = ""
                                                viewModel.updateActiveConfigPayload("")
                                                Toast.makeText(context, "Payload cleared (Raw TCP Direct mode)", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text("Clear", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }

                            // Macro Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(listOf("[host]", "[port]", "[host_port]", "[protocol]", "[crlf]", "[split]", "[ua]")) { chip ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DarkSurfaceElevated)
                                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                            .clickable { localPayload += chip }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(chip, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyanPrimary)
                                    }
                                }
                            }

                            // Monospace Text Field
                            OutlinedTextField(
                                value = localPayload,
                                onValueChange = { localPayload = it },
                                label = { Text("HTTP Payload String") },
                                placeholder = { Text("CONNECT [host_port] [protocol][crlf]Host: [host][crlf][crlf]") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Injected on CONNECT before SSH",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                                Button(
                                    onClick = {
                                        viewModel.updateActiveConfigPayload(localPayload)
                                        Toast.makeText(context, "Payload saved for '${cfg.name}'!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        tint = Color(0xFF001E2B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Payload", color = Color(0xFF001E2B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
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

    // Quick Server Setup Dialog (Host, Port, User, Password, Protocol)
    val activeCfg = activeConfig ?: configs.firstOrNull()
    if (showQuickServerSetup && activeCfg != null) {
        var host by remember(activeCfg.id) { mutableStateOf(activeCfg.serverHost) }
        var port by remember(activeCfg.id) { mutableStateOf(activeCfg.serverPort.toString()) }
        var user by remember(activeCfg.id) { mutableStateOf(activeCfg.username) }
        var pass by remember(activeCfg.id) { mutableStateOf(activeCfg.password) }
        var proto by remember(activeCfg.id) { mutableStateOf(activeCfg.protocol) }

        AlertDialog(
            onDismissRequest = { showQuickServerSetup = false },
            containerColor = DarkSurface,
            title = {
                Text("Server Config Setup", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your real SSH/VPN server credentials to establish the connection.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    // Protocol Selector Chips
                    Text("Protocol:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(TunnelProtocol.SSH_DIRECT, TunnelProtocol.SSH_SSL, TunnelProtocol.SSH_HTTP_PAYLOAD, TunnelProtocol.VMESS, TunnelProtocol.VLESS, TunnelProtocol.TROJAN)) { p ->
                            val isSel = proto == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) CyanPrimary else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) CyanPrimary else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { proto = p }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(p.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFF001E2B) else TextSecondary)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Server Host / IP") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = user,
                            onValueChange = { user = it },
                            label = { Text("SSH User") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = pass,
                            onValueChange = { pass = it },
                            label = { Text("Password") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = port.toIntOrNull() ?: proto.defaultPort
                        val updated = activeCfg.copy(
                            serverHost = host.ifBlank { "127.0.0.1" },
                            serverPort = p,
                            username = user,
                            password = pass,
                            protocol = proto
                        )
                        viewModel.saveConfig(updated)
                        showQuickServerSetup = false
                        Toast.makeText(context, "Server config updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Save & Apply", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickServerSetup = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
