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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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

enum class ToolTab(val title: String) {
    HOST_CHECKER("Host Checker"),
    IP_HUNTER("IP Hunter"),
    PING("Ping Test"),
    DNS("DNS Tools"),
    LOGS("Live Logs")
}

@Composable
fun ToolsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ToolTab.HOST_CHECKER) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Network & Diagnostic Tools",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tool Tabs
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ToolTab.values()) { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanPrimary else DarkSurfaceElevated)
                        .border(1.dp, if (isSelected) CyanPrimary else DarkBorder, RoundedCornerShape(8.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF001E2B) else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            ToolTab.HOST_CHECKER -> HostCheckerSection(viewModel)
            ToolTab.IP_HUNTER -> IpHunterSection(viewModel)
            ToolTab.PING -> PingSection(viewModel)
            ToolTab.DNS -> DnsSection(viewModel)
            ToolTab.LOGS -> LiveLogsSection(viewModel)
        }
    }
}

@Composable
private fun HostCheckerSection(viewModel: MainViewModel) {
    var targetUrl by remember { mutableStateOf("cloudflare.com") }
    var method by remember { mutableStateOf("HEAD") }
    var proxyHost by remember { mutableStateOf("") }
    var proxyPort by remember { mutableStateOf("8080") }

    val result by viewModel.hostCheckResult.collectAsState()
    val isLoading by viewModel.isHostChecking.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Host / SNI Bug Tester", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)

                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = { targetUrl = it },
                        label = { Text("URL or Domain (e.g., example.com)") },
                        singleLine = true,
                        colors = toolsTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("host_checker_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HEAD", "GET", "POST").forEach { m ->
                            val isSel = method == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) ElectricBlue else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) ElectricBlue else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { method = m }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(m, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else TextSecondary)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proxyHost,
                            onValueChange = { proxyHost = it },
                            label = { Text("Proxy Host (Optional)") },
                            colors = toolsTextFieldColors(),
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = proxyPort,
                            onValueChange = { proxyPort = it },
                            label = { Text("Port") },
                            colors = toolsTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val p = proxyPort.toIntOrNull() ?: 8080
                            viewModel.runHostCheck(targetUrl, method, "DarkTunnel/1.0", proxyHost, p)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("run_host_check_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF001E2B))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isLoading) "Checking Host..." else "Check Host", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        result?.let { res ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, if (res.isSuccess) NeonGreen else CrimsonError, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HTTP ${res.statusCode} ${res.statusMessage}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (res.isSuccess) NeonGreen else CrimsonError
                            )
                            Text(
                                text = "${res.responseTimeMs} ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Response Headers:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        res.headers.forEach { (k, v) ->
                            Text(
                                text = "$k: $v",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IpHunterSection(viewModel: MainViewModel) {
    var prefixText by remember { mutableStateOf("10., 100., 172.") }
    val result by viewModel.ipHunterResult.collectAsState()
    val isLoading by viewModel.isHuntingIp.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("IP Hunter / Subnet Matcher", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
                    Text(
                        text = "Scans current public IP and alerts when it matches required ISP subnet prefix for payload injection.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = prefixText,
                        onValueChange = { prefixText = it },
                        label = { Text("Target IP Subnet Prefixes (comma separated)") },
                        colors = toolsTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("ip_hunter_input")
                    )

                    Button(
                        onClick = {
                            val list = prefixText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            viewModel.runIpHunter(list)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("run_ip_hunter_button")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF001E2B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isLoading) "Detecting IP..." else "Hunt IP", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        result?.let { res ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, if (res.isMatched) NeonGreen else CrimsonError, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Current Public IP:", fontSize = 11.sp, color = TextMuted)
                        Text(res.currentIp, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (res.isMatched) "✓ MATCHED TARGET PREFIX!" else "✗ IP does not match target prefix. Toggle Airplane mode to renew IP.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (res.isMatched) NeonGreen else CrimsonError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PingSection(viewModel: MainViewModel) {
    var host by remember { mutableStateOf("1.1.1.1") }
    var port by remember { mutableStateOf("80") }
    val results by viewModel.pingResults.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TCP Latency & Ping Tester", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host or IP") },
                            colors = toolsTextFieldColors(),
                            modifier = Modifier.weight(2f).testTag("ping_host_input")
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            colors = toolsTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val p = port.toIntOrNull() ?: 80
                            viewModel.runPing(host, p)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("run_ping_button")
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color(0xFF001E2B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPinging) "Pinging..." else "Start Ping Test", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(results) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Seq ${item.sequence}: ${item.host}", fontSize = 12.sp, color = TextPrimary)
                    Text(
                        text = if (item.isSuccess) "${item.rttMs} ms" else "Timeout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSuccess) NeonGreen else CrimsonError
                    )
                }
            }
        }
    }
}

@Composable
private fun DnsSection(viewModel: MainViewModel) {
    var domain by remember { mutableStateOf("google.com") }
    var resolver by remember { mutableStateOf("8.8.8.8") }
    val result by viewModel.dnsResult.collectAsState()
    val isQuerying by viewModel.isQueryingDns.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("DNS & DoH Lookup Tool", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)

                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Domain Name (e.g. google.com)") },
                        colors = toolsTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("dns_domain_input")
                    )

                    OutlinedTextField(
                        value = resolver,
                        onValueChange = { resolver = it },
                        label = { Text("DNS Resolver IP") },
                        colors = toolsTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.runDnsQuery(domain, resolver) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resolve DNS", color = Color(0xFF001E2B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.runDohQuery(domain) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Query DoH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        result?.let { res ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, if (res.isSuccess) NeonGreen else CrimsonError, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Resolved via ${res.resolver} (${res.queryTimeMs} ms)", fontSize = 11.sp, color = TextMuted)
                        if (res.isSuccess) {
                            res.resolvedIps.forEach { ip ->
                                Text("• $ip", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        } else {
                            Text("Resolution failed: ${res.error ?: "Unknown error"}", fontSize = 12.sp, color = CrimsonError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveLogsSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }

    val filtered = if (selectedLevel == null) logs else logs.filter { it.level == selectedLevel }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filtered.size} Log Entries", fontSize = 12.sp, color = TextMuted)
            Row {
                IconButton(onClick = {
                    val fullText = logs.joinToString("\n") { "[${it.level}] ${it.message}" }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Dark Tunnel Logs", fullText))
                    Toast.makeText(context, "Logs copied!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = CrimsonError, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070B12))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (filtered.isEmpty()) {
                Text(
                    text = "No logs yet. Connect to a server or run diagnostics to see live events.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { entry ->
                        val color = when (entry.level) {
                            LogLevel.ERROR -> CrimsonError
                            LogLevel.WARN -> AmberWarning
                            LogLevel.SUCCESS -> NeonGreen
                            LogLevel.DEBUG -> TextMuted
                            LogLevel.INFO -> CyanPrimary
                        }
                        Text(
                            text = "[${entry.level.name}] ${entry.message}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun toolsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedContainerColor = DarkSurfaceElevated,
    unfocusedContainerColor = DarkSurfaceElevated,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
