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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.PayloadInjectMethod
import com.example.model.PayloadRequestMethod
import com.example.model.PayloadTemplate
import com.example.tunnel.NetworkToolsService
import com.example.tunnel.PayloadFormatter
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
import kotlinx.coroutines.launch

@Composable
fun PayloadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedPayloads by viewModel.payloads.collectAsState()

    var targetHost by remember { mutableStateOf("[host]") }
    var targetPort by remember { mutableStateOf("[port]") }
    var proxyHost by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PayloadRequestMethod.CONNECT) }
    var selectedInject by remember { mutableStateOf(PayloadInjectMethod.NORMAL) }

    // Header toggles
    var onlineHost by remember { mutableStateOf(true) }
    var forwardHost by remember { mutableStateOf(false) }
    var reverseProxy by remember { mutableStateOf(false) }
    var keepAlive by remember { mutableStateOf(true) }
    var userAgent by remember { mutableStateOf(true) }
    var splitPayload by remember { mutableStateOf(false) }

    var payloadText by remember {
        mutableStateOf("CONNECT [host_port] [protocol][crlf]Host: [host][crlf]X-Online-Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]")
    }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val macroChips = listOf(
        "[host]", "[port]", "[host_port]", "[protocol]",
        "[crlf]", "[split]", "[ua]", "[cr]", "[lf]"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payload Generator",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Construct custom HTTP/WebSocket tunneling headers",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Generator Box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GENERATOR CONTROLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                            letterSpacing = 1.sp
                        )
                        Button(
                            onClick = {
                                payloadText = PayloadFormatter.generatePayload(
                                    method = selectedMethod.verb,
                                    targetHost = targetHost,
                                    targetPort = 80,
                                    proxyHost = proxyHost,
                                    injectMethod = selectedInject.displayName,
                                    enableKeepAlive = keepAlive,
                                    enableOnlineHost = onlineHost,
                                    enableForwardHost = forwardHost,
                                    enableReverseProxy = reverseProxy,
                                    enableUserAgent = userAgent,
                                    splitPayload = splitPayload
                                )
                                Toast.makeText(context, "Payload generated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF001E2B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Generate", color = Color(0xFF001E2B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Request Method Chips
                    Text("Request Method", fontSize = 11.sp, color = TextMuted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PayloadRequestMethod.values()) { m ->
                            val isSel = selectedMethod == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) CyanPrimary else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) CyanPrimary else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedMethod = m }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = m.verb,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF001E2B) else TextSecondary
                                )
                            }
                        }
                    }

                    // Injection Method Chips
                    Text("Injection Method", fontSize = 11.sp, color = TextMuted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PayloadInjectMethod.values()) { inj ->
                            val isSel = selectedInject == inj
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) ElectricBlue else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) ElectricBlue else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedInject = inj }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = inj.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    // Header Checkboxes
                    Text("Header Injections", fontSize = 11.sp, color = TextMuted)
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            HeaderCheckboxItem("Online Host", onlineHost, { onlineHost = it }, Modifier.weight(1f))
                            HeaderCheckboxItem("Keep-Alive", keepAlive, { keepAlive = it }, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            HeaderCheckboxItem("User-Agent", userAgent, { userAgent = it }, Modifier.weight(1f))
                            HeaderCheckboxItem("Split", splitPayload, { splitPayload = it }, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            HeaderCheckboxItem("Forward Host", forwardHost, { forwardHost = it }, Modifier.weight(1f))
                            HeaderCheckboxItem("Reverse Proxy", reverseProxy, { reverseProxy = it }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Payload Text Editor & Helper Chips
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAYLOAD EDITOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                            letterSpacing = 1.sp
                        )
                        Row {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Dark Tunnel Payload", payloadText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Payload copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                val template = PayloadTemplate(
                                    name = "Custom Payload ${savedPayloads.size + 1}",
                                    description = "User created payload template",
                                    payloadString = payloadText,
                                    injectMethod = selectedInject,
                                    requestMethod = selectedMethod,
                                    isCustom = true
                                )
                                viewModel.savePayload(template)
                                Toast.makeText(context, "Saved to templates!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = "Save", tint = NeonGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Helper Macro Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(macroChips) { macro ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                    .clickable { payloadText += macro }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = macro,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanPrimary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payloadText,
                        onValueChange = { payloadText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payload_editor_textarea"),
                        minLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )

                    // Test Payload Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isTesting = true
                                testStatus = "Testing payload structure..."
                                scope.launch {
                                    // Parse and test
                                    val chunks = PayloadFormatter.format(payloadText, "1.1.1.1", 80)
                                    testStatus = "Valid syntax: ${chunks.size} chunk(s), ${chunks.sumOf { it.length }} bytes."
                                    isTesting = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTesting) "Testing..." else "Test Payload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        testStatus?.let { msg ->
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = NeonGreen,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Templates Library
        item {
            Text(
                text = "SAVED TEMPLATES (${savedPayloads.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }

        items(savedPayloads) { tmpl ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        payloadText = tmpl.payloadString
                        Toast.makeText(context, "Loaded '${tmpl.name}' into editor", Toast.LENGTH_SHORT).show()
                    }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tmpl.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (!tmpl.isCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyanPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("Preset", fontSize = 9.sp, color = CyanPrimary)
                                }
                            }
                        }
                        Text(
                            text = tmpl.description,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Load",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = CyanPrimary,
                checkmarkColor = Color(0xFF001E2B),
                uncheckedColor = DarkBorder
            )
        )
        Text(title, fontSize = 11.sp, color = TextPrimary)
    }
}
