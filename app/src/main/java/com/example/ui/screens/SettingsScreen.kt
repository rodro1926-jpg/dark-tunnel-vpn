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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DefaultServers
import com.example.ui.MainViewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = viewModel.preferences

    var autoReconnect by remember { mutableStateOf(prefs.autoReconnect) }
    var enableIpv6 by remember { mutableStateOf(prefs.enableIpv6) }
    var enableKillSwitch by remember { mutableStateOf(prefs.enableKillSwitch) }
    var wakeLock by remember { mutableStateOf(prefs.wakeLockEnabled) }

    var timeoutSeconds by remember { mutableFloatStateOf(prefs.connectionTimeoutSeconds.toFloat()) }
    var keepAliveSeconds by remember { mutableFloatStateOf(prefs.keepAliveIntervalSeconds.toFloat()) }
    var mtuValue by remember { mutableIntStateOf(prefs.mtu) }

    var selectedDnsName by remember { mutableStateOf(prefs.selectedDnsName) }
    var customDnsInput by remember { mutableStateOf(prefs.customDns) }
    var showDnsDialog by remember { mutableStateOf(false) }

    var providerBrand by remember { mutableStateOf(prefs.providerBrandName) }
    var providerMsg by remember { mutableStateOf(prefs.providerMessage) }

    val dnsPresets = listOf(
        "Google DNS (8.8.8.8)" to "8.8.8.8",
        "Cloudflare DNS (1.1.1.1)" to "1.1.1.1",
        "Quad9 Security (9.9.9.9)" to "9.9.9.9",
        "AdGuard DNS (94.140.14.14)" to "94.140.14.14",
        "OpenDNS (208.67.222.222)" to "208.67.222.222",
        "Custom DNS" to customDnsInput
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preferences & Engine Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Configure tunnel timeouts, MTU, DNS, and networking protocols",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        // Section 1: Tunnel Connectivity
        item {
            SettingsCategoryCard("CONNECTION & TUNNELING") {
                SettingsSwitchRow(
                    title = "Auto Reconnect",
                    subtitle = "Automatically re-establish tunnel on connection drop",
                    checked = autoReconnect,
                    onCheckedChange = {
                        autoReconnect = it
                        prefs.autoReconnect = it
                    }
                )

                SettingsSliderRow(
                    title = "Connection Timeout",
                    value = "${timeoutSeconds.toInt()}s",
                    currentValue = timeoutSeconds,
                    valueRange = 5f..60f,
                    onValueChange = {
                        timeoutSeconds = it
                        prefs.connectionTimeoutSeconds = it.toInt()
                    }
                )

                SettingsSliderRow(
                    title = "Keep-Alive Heartbeat",
                    value = "${keepAliveSeconds.toInt()}s",
                    currentValue = keepAliveSeconds,
                    valueRange = 5f..60f,
                    onValueChange = {
                        keepAliveSeconds = it
                        prefs.keepAliveIntervalSeconds = it.toInt()
                    }
                )

                // MTU Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tunnel MTU", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Maximum Transmission Unit size", fontSize = 11.sp, color = TextMuted)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1280, 1400, 1500).forEach { m ->
                            val isSel = mtuValue == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) CyanPrimary else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) CyanPrimary else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        mtuValue = m
                                        prefs.mtu = m
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "$m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF001E2B) else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: DNS & Routing
        item {
            SettingsCategoryCard("DNS & ROUTING") {
                // DNS Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDnsDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Primary DNS Resolver", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(selectedDnsName, fontSize = 11.sp, color = CyanPrimary)
                    }
                    Text("Change", fontSize = 12.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                }

                SettingsSwitchRow(
                    title = "Enable IPv6 Routing",
                    subtitle = "Route IPv6 alongside standard IPv4 tunnel",
                    checked = enableIpv6,
                    onCheckedChange = {
                        enableIpv6 = it
                        prefs.enableIpv6 = it
                    }
                )

                SettingsSwitchRow(
                    title = "Kill Switch",
                    subtitle = "Block device traffic if VPN drops unexpectedly",
                    checked = enableKillSwitch,
                    onCheckedChange = {
                        enableKillSwitch = it
                        prefs.enableKillSwitch = it
                    }
                )

                SettingsSwitchRow(
                    title = "CPU WakeLock",
                    subtitle = "Prevents Android OS from sleeping tunnel threads",
                    checked = wakeLock,
                    onCheckedChange = {
                        wakeLock = it
                        prefs.wakeLockEnabled = it
                    }
                )
            }
        }

        // Section 3: Provider Mode Branding
        item {
            SettingsCategoryCard("PROVIDER MODE DEFAULTS") {
                OutlinedTextField(
                    value = providerBrand,
                    onValueChange = {
                        providerBrand = it
                        prefs.providerBrandName = it
                    },
                    label = { Text("Provider Brand Name") },
                    colors = settingsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = providerMsg,
                    onValueChange = {
                        providerMsg = it
                        prefs.providerMessage = it
                    },
                    label = { Text("Provider Message") },
                    colors = settingsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 4: Maintenance
        item {
            SettingsCategoryCard("DATA & RESTORE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearLogs()
                            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Logs", fontSize = 12.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                viewModel.database.vpnConfigDao().insertAll(DefaultServers.INITIAL_CONFIGS)
                                viewModel.database.payloadDao().insertAll(DefaultServers.DEFAULT_PAYLOADS)
                            }
                            Toast.makeText(context, "Default servers reloaded", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Servers", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // Section 5: About & Legal
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dark Tunnel VPN v1.0.0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(
                        text = "Encrypted network tunneling client for Android. Compatible with SSH, Stunnel, HTTP Payloads, Xray VMess/VLESS, Trojan, and SlowDNS.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Notice: Only connect to remote servers and endpoints that you own, operate, or are explicitly authorized to use.",
                        fontSize = 10.sp,
                        color = AmberWarning
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DNS Selection Dialog
    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            containerColor = DarkSurface,
            title = { Text("Choose DNS Resolver", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dnsPresets.forEach { (name, ip) ->
                        val isSel = selectedDnsName == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyanPrimary.copy(alpha = 0.15f) else DarkSurfaceElevated)
                                .clickable {
                                    selectedDnsName = name
                                    prefs.selectedDnsName = name
                                    if (name != "Custom DNS") {
                                        prefs.selectedDns = ip
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = 13.sp, color = if (isSel) CyanPrimary else TextPrimary, fontWeight = FontWeight.Medium)
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (selectedDnsName == "Custom DNS") {
                        OutlinedTextField(
                            value = customDnsInput,
                            onValueChange = {
                                customDnsInput = it
                                prefs.customDns = it
                                prefs.selectedDns = it
                            },
                            label = { Text("Custom DNS IP") },
                            colors = settingsTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDnsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("OK", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    categoryTitle: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = categoryTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary,
                letterSpacing = 1.sp
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
        )
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    value: String,
    currentValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
        }
        Slider(
            value = currentValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = CyanPrimary,
                activeTrackColor = CyanPrimary,
                inactiveTrackColor = DarkBorder
            )
        )
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedContainerColor = DarkSurfaceElevated,
    unfocusedContainerColor = DarkSurfaceElevated,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
