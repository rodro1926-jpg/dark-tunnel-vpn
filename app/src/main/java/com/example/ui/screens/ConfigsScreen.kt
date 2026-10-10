package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InjectionMode
import com.example.model.TunnelProtocol
import com.example.model.VpnConfig
import com.example.tunnel.ConfigParser
import com.example.ui.MainViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConfigsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configs by viewModel.configs.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var configToEdit by remember { mutableStateOf<VpnConfig?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<VpnConfig?>(null) }

    val filterOptions = listOf("ALL", "SSH", "VMESS", "VLESS", "TROJAN", "SOCKS5")

    val filteredConfigs = configs.filter { cfg ->
        val matchesCategory = when (selectedFilter) {
            "SSH" -> cfg.protocol.name.startsWith("SSH")
            "VMESS" -> cfg.protocol == TunnelProtocol.VMESS
            "VLESS" -> cfg.protocol == TunnelProtocol.VLESS
            "TROJAN" -> cfg.protocol == TunnelProtocol.TROJAN
            "SOCKS5" -> cfg.protocol == TunnelProtocol.SOCKS5
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                cfg.name.contains(searchQuery, ignoreCase = true) ||
                cfg.serverHost.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Config Manager",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${configs.size} Profiles Available",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Row {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Import",
                            tint = CyanPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or server IP...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { opt ->
                    val isSelected = selectedFilter == opt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyanPrimary else DarkSurfaceElevated)
                            .border(1.dp, if (isSelected) CyanPrimary else DarkBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = opt }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = opt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF001E2B) else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Configs List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredConfigs, key = { it.id }) { cfg ->
                    ConfigItemCard(
                        config = cfg,
                        isActive = (activeConfig?.id ?: "") == cfg.id,
                        onSelect = { viewModel.selectConfig(cfg) },
                        onEdit = { configToEdit = cfg },
                        onDuplicate = { viewModel.duplicateConfig(cfg) },
                        onExport = { showExportDialog = cfg },
                        onToggleFavorite = { viewModel.saveConfig(cfg.copy(isFavorite = !cfg.isFavorite)) },
                        onDelete = { viewModel.deleteConfig(cfg) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button to Add New Config
        FloatingActionButton(
            onClick = {
                configToEdit = VpnConfig(
                    name = "New Custom Server",
                    serverHost = "127.0.0.1",
                    serverPort = 443,
                    protocol = TunnelProtocol.SSH_SSL
                )
            },
            containerColor = CyanPrimary,
            contentColor = Color(0xFF001E2B),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_config_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Config")
        }
    }

    // Edit / Create Dialog
    configToEdit?.let { editingConfig ->
        EditConfigDialog(
            initialConfig = editingConfig,
            onDismiss = { configToEdit = null },
            onSave = { updated ->
                viewModel.saveConfig(updated)
                configToEdit = null
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        ImportConfigDialog(
            onDismiss = { showImportDialog = false },
            onImport = { raw, pass ->
                viewModel.importConfig(raw, pass) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showImportDialog = false
                }
            }
        )
    }

    // Export Dialog (Plain / Provider Encrypted)
    showExportDialog?.let { expConfig ->
        ExportConfigDialog(
            config = expConfig,
            onDismiss = { showExportDialog = null }
        )
    }
}

@Composable
fun ConfigItemCard(
    config: VpnConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) CyanPrimary.copy(alpha = 0.08f) else DarkSurfaceCard)
            .border(
                1.5.dp,
                if (isActive) CyanPrimary else DarkBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) CyanPrimary else TextPrimary
                    )
                    if (config.isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = config.protocol.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )
                    }
                    if (config.payload.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+ Payload",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${config.serverHost}:${config.serverPort}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                if (config.expiryTimestamp > 0L) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(config.expiryTimestamp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Expires: $dateStr",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (config.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (config.isFavorite) Color(0xFFFFD700) else TextMuted
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Profile", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyanPrimary) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ElectricBlue) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export / Share", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = NeonGreen) },
                            onClick = {
                                showMenu = false
                                onExport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditConfigDialog(
    initialConfig: VpnConfig,
    onDismiss: () -> Unit,
    onSave: (VpnConfig) -> Unit
) {
    var name by remember { mutableStateOf(initialConfig.name) }
    var host by remember { mutableStateOf(initialConfig.serverHost) }
    var port by remember { mutableStateOf(initialConfig.serverPort.toString()) }
    var username by remember { mutableStateOf(initialConfig.username) }
    var password by remember { mutableStateOf(initialConfig.password) }
    var selectedProtocol by remember { mutableStateOf(initialConfig.protocol) }
    var selectedInjection by remember { mutableStateOf(initialConfig.injectionMode) }
    var sni by remember { mutableStateOf(initialConfig.sniHost) }
    var proxyHost by remember { mutableStateOf(initialConfig.httpProxyHost) }
    var proxyPort by remember { mutableStateOf(if (initialConfig.httpProxyPort > 0) initialConfig.httpProxyPort.toString() else "8080") }
    var payload by remember { mutableStateOf(initialConfig.payload) }
    var uuid by remember { mutableStateOf(initialConfig.uuid) }
    var path by remember { mutableStateOf(initialConfig.path) }
    var isPayloadVisible by remember { mutableStateOf(initialConfig.payload.isNotBlank() || initialConfig.protocol == TunnelProtocol.SSH_HTTP_PAYLOAD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Edit Configuration", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Server Host / IP") },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Protocol selector chips
                Text("Protocol Engine", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TunnelProtocol.values()) { proto ->
                        val isSel = selectedProtocol == proto
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyanPrimary else DarkSurfaceElevated)
                                .border(1.dp, if (isSel) CyanPrimary else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedProtocol = proto }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = proto.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color(0xFF001E2B) else TextSecondary
                            )
                        }
                    }
                }

                // Protocol specific fields
                when (selectedProtocol) {
                    TunnelProtocol.SSH_DIRECT, TunnelProtocol.SSH_SSL, TunnelProtocol.SSH_HTTP_PAYLOAD, TunnelProtocol.SSH_DNSTT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("SSH User") },
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("SSH Password") },
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    TunnelProtocol.VMESS, TunnelProtocol.VLESS -> {
                        OutlinedTextField(
                            value = uuid,
                            onValueChange = { uuid = it },
                            label = { Text("UUID") },
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = path,
                            onValueChange = { path = it },
                            label = { Text("WebSocket Path") },
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TunnelProtocol.TROJAN, TunnelProtocol.SHADOWSOCKS -> {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password / Token") },
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }

                OutlinedTextField(
                    value = sni,
                    onValueChange = { sni = it },
                    label = { Text("SNI (Server Name Indication)") },
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedProtocol == TunnelProtocol.SSH_DIRECT ||
                    selectedProtocol == TunnelProtocol.SSH_SSL ||
                    selectedProtocol == TunnelProtocol.SSH_HTTP_PAYLOAD
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isPayloadVisible) CyanPrimary.copy(alpha = 0.5f) else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPayloadVisible = !isPayloadVisible },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = if (isPayloadVisible) CyanPrimary else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "HTTP Payload (Custom Injection)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isPayloadVisible) "Showing payload (Tap to Hide)" else "Hidden (Tap to Show & Configure)",
                                            fontSize = 10.sp,
                                            color = if (isPayloadVisible) CyanPrimary else TextMuted
                                        )
                                    }
                                }
                                Switch(
                                    checked = isPayloadVisible,
                                    onCheckedChange = { isPayloadVisible = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                                )
                            }

                            if (isPayloadVisible) {
                                // Preset Templates
                                Text("Quick Templates:", fontSize = 11.sp, color = TextMuted)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CyanPrimary.copy(alpha = 0.15f))
                                                .clickable {
                                                    payload = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Keep-Alive[crlf]User-Agent: [ua][crlf][crlf]"
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
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
                                                    payload = "CONNECT [host_port] [protocol][crlf]Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]"
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
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
                                                    payload = "GET http://[host]/ [protocol][crlf]Host: [host][crlf][split]CONNECT [host_port] [protocol][crlf][crlf]"
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text("Front Split", fontSize = 10.sp, color = NeonGreen)
                                        }
                                    }
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(DarkBorder)
                                                .clickable { payload = "" }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text("Clear", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }

                                // Helper Macro Chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(listOf("[host]", "[port]", "[host_port]", "[protocol]", "[crlf]", "[split]", "[ua]")) { chip ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(DarkSurface)
                                                .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                                .clickable { payload += chip }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(chip, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyanPrimary)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = payload,
                                    onValueChange = { payload = it },
                                    label = { Text("HTTP Payload String") },
                                    placeholder = { Text("CONNECT [host_port] [protocol][crlf]Host: [host][crlf][crlf]") },
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                )

                                if (selectedProtocol == TunnelProtocol.SSH_HTTP_PAYLOAD) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = proxyHost,
                                            onValueChange = { proxyHost = it },
                                            label = { Text("Proxy Host") },
                                            colors = dialogTextFieldColors(),
                                            modifier = Modifier.weight(2f)
                                        )
                                        OutlinedTextField(
                                            value = proxyPort,
                                            onValueChange = { proxyPort = it },
                                            label = { Text("Proxy Port") },
                                            colors = dialogTextFieldColors(),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: selectedProtocol.defaultPort
                    val pp = proxyPort.toIntOrNull() ?: 8080
                    val updated = initialConfig.copy(
                        name = name.ifBlank { "Server" },
                        serverHost = host.ifBlank { "127.0.0.1" },
                        serverPort = p,
                        username = username,
                        password = password,
                        protocol = selectedProtocol,
                        injectionMode = selectedInjection,
                        sniHost = sni,
                        httpProxyHost = proxyHost,
                        httpProxyPort = pp,
                        payload = payload,
                        uuid = uuid,
                        path = path
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Save", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun ImportConfigDialog(
    onDismiss: () -> Unit,
    onImport: (String, String?) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Import Configuration", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste VMess / VLESS / Trojan / Shadowsocks / SSH URL, raw JSON, or DARKTUNNEL encrypted package.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("vmess://, vless://, trojan://, DARKTUNNEL:...", color = TextMuted, fontSize = 12.sp) },
                    minLines = 4,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (rawText.trim().startsWith("DARKTUNNEL:")) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Decryption Password (if protected)") },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(rawText, password.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Import", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun ExportConfigDialog(
    config: VpnConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isProviderMode by remember { mutableStateOf(false) }
    var providerPass by remember { mutableStateOf("") }
    var providerName by remember { mutableStateOf(config.providerName.ifBlank { "Dark Tunnel Network" }) }
    var providerNote by remember { mutableStateOf(config.providerNote.ifBlank { "Authorized users only." }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Export & Share Configuration", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Provider Mode (Encrypted .dark)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Locks config and encrypts payload with AES-256", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = isProviderMode,
                        onCheckedChange = { isProviderMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }

                if (isProviderMode) {
                    OutlinedTextField(
                        value = providerPass,
                        onValueChange = { providerPass = it },
                        label = { Text("Encryption Password (optional)") },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text("Provider Brand Name") },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = providerNote,
                        onValueChange = { providerNote = it },
                        label = { Text("Provider Note / Expiry Message") },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val exportText = if (isProviderMode) {
                        val lockedCfg = config.copy(
                            isLocked = true,
                            providerName = providerName,
                            providerNote = providerNote
                        )
                        ConfigParser.encryptDarkConfig(lockedCfg, providerPass)
                    } else {
                        ConfigParser.toJson(config)
                    }

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Dark Tunnel Config", exportText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied configuration to clipboard!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Copy to Clipboard", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted)
            }
        }
    )
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedContainerColor = DarkSurfaceElevated,
    unfocusedContainerColor = DarkSurfaceElevated,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
