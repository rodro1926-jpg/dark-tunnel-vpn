package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppPreferences
import com.example.data.VpnDatabase
import com.example.model.TrafficStats
import com.example.model.VpnConfig
import com.example.model.VpnStatus
import com.example.tunnel.ConfigParser
import com.example.tunnel.TunnelConnection
import com.example.tunnel.TunnelEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class DarkTunnelVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelConnection: TunnelConnection? = null
    private var statsJob: Job? = null
    private var tunnelJob: Job? = null

    private val isRunning = AtomicBoolean(false)
    private val bytesIn = AtomicLong(0L)
    private val bytesOut = AtomicLong(0L)
    private var startTimeMillis = 0L

    private lateinit var preferences: AppPreferences
    private lateinit var database: VpnDatabase
    private var currentConfig: VpnConfig? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "dark_tunnel_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        database = VpnDatabase.getDatabase(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            VpnController.ACTION_CONNECT -> {
                val configId = intent.getStringExtra(VpnController.EXTRA_CONFIG_ID)
                serviceScope.launch {
                    val config = if (configId != null) {
                        database.vpnConfigDao().getConfigById(configId)
                    } else {
                        null
                    }
                    if (config != null) {
                        startTunnel(config)
                    } else {
                        AppLogger.e("Configuration not found!")
                        stopTunnel(VpnStatus.ERROR)
                    }
                }
            }
            VpnController.ACTION_DISCONNECT -> {
                AppLogger.i("User requested tunnel disconnect.")
                serviceScope.launch {
                    stopTunnel(VpnStatus.DISCONNECTED)
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startTunnel(config: VpnConfig) {
        currentConfig = config
        VpnController.setActiveConfig(config)
        VpnController.updateStatus(VpnStatus.CONNECTING)
        AppLogger.i("Starting Dark Tunnel engine for [${config.name}]...")

        // Validate config
        val validationError = ConfigParser.validateConfig(config)
        if (validationError != null) {
            AppLogger.e("Validation Failed: $validationError")
            VpnController.updateStatus(VpnStatus.ERROR)
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification("Connecting to ${config.name}..."))

        try {
            // 1. Establish real protocol tunnel
            val connection = withContext(Dispatchers.IO) {
                TunnelEngine.establishTunnel(
                    config = config,
                    vpnService = this@DarkTunnelVpnService,
                    timeoutMs = preferences.connectionTimeoutSeconds * 1000
                )
            }
            tunnelConnection = connection

            // 2. Establish TUN interface
            val tun = establishTunInterface(config)
            if (tun == null) {
                AppLogger.e("Failed to establish TUN interface!")
                connection.close()
                stopTunnel(VpnStatus.ERROR)
                return
            }
            vpnInterface = tun
            isRunning.set(true)
            startTimeMillis = System.currentTimeMillis()

            AppLogger.s("TUN interface established (MTU: ${preferences.mtu}). VPN is LIVE!")
            VpnController.updateStatus(VpnStatus.CONNECTED)
            startForeground(NOTIFICATION_ID, buildNotification("Connected: ${config.name} (${config.protocol.displayName})"))

            // 3. Start packet routing loop
            startPacketLoop(tun, connection)

            // 4. Start statistics & telemetry loop
            startStatsMonitoring(config)

        } catch (e: Exception) {
            AppLogger.e("Connection error: ${e.message ?: e.javaClass.simpleName}")
            stopTunnel(VpnStatus.ERROR)
        }
    }

    private fun establishTunInterface(config: VpnConfig): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("Dark Tunnel: ${config.name}")
            .setMtu(preferences.mtu)
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)

        // DNS Server
        val dnsServer = if (config.customDns.isNotBlank()) config.customDns else preferences.selectedDns
        try {
            builder.addDnsServer(dnsServer)
            AppLogger.d("Configured primary DNS: $dnsServer")
        } catch (e: Exception) {
            builder.addDnsServer("8.8.8.8")
        }

        // IPv6 support if enabled
        if (preferences.enableIpv6) {
            try {
                builder.addAddress("fd00:1::2", 64)
                builder.addRoute("::", 0)
                AppLogger.d("IPv6 routing enabled.")
            } catch (e: Exception) {
                AppLogger.w("Could not assign IPv6: ${e.message}")
            }
        }

        // Per-App VPN filter
        if (preferences.perAppVpnEnabled && preferences.selectedApps.isNotEmpty()) {
            val isExclude = preferences.perAppExcludeMode
            AppLogger.d("Applying Per-App VPN filter (${if (isExclude) "Exclude" else "Include"} ${preferences.selectedApps.size} apps)")
            for (pkg in preferences.selectedApps) {
                try {
                    if (isExclude) {
                        builder.addDisallowedApplication(pkg)
                    } else {
                        builder.addAllowedApplication(pkg)
                    }
                } catch (e: Exception) {
                    AppLogger.w("Could not configure package filter for $pkg: ${e.message}")
                }
            }
        }

        return try {
            builder.establish()
        } catch (e: Exception) {
            AppLogger.e("Builder.establish() threw: ${e.message}")
            null
        }
    }

    private fun startPacketLoop(tun: ParcelFileDescriptor, tunnel: TunnelConnection) {
        tunnelJob = serviceScope.launch(Dispatchers.IO) {
            val tunInput = FileInputStream(tun.fileDescriptor)
            val tunOutput = FileOutputStream(tun.fileDescriptor)
            val tunnelOut = tunnel.outputStream
            val tunnelIn = tunnel.inputStream

            val packetBuffer = ByteArray(preferences.mtu)

            // Thread 1: TUN -> Tunnel
            val uplink = launch {
                try {
                    while (isActive && isRunning.get()) {
                        val length = tunInput.read(packetBuffer)
                        if (length > 0) {
                            bytesOut.addAndGet(length.toLong())
                            tunnelOut.write(packetBuffer, 0, length)
                            tunnelOut.flush()
                        } else if (length == -1) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        AppLogger.w("Uplink read ended: ${e.message}")
                    }
                }
            }

            // Thread 2: Tunnel -> TUN
            val downlink = launch {
                try {
                    val incomingBuffer = ByteArray(preferences.mtu)
                    while (isActive && isRunning.get()) {
                        val length = tunnelIn.read(incomingBuffer)
                        if (length > 0) {
                            bytesIn.addAndGet(length.toLong())
                            tunOutput.write(incomingBuffer, 0, length)
                        } else if (length == -1) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        AppLogger.w("Downlink read ended: ${e.message}")
                    }
                }
            }

            uplink.join()
            downlink.join()

            if (isRunning.get()) {
                AppLogger.w("Tunnel streams terminated unexpectedly.")
                if (preferences.autoReconnect) {
                    AppLogger.i("Auto-reconnect triggered...")
                    VpnController.updateStatus(VpnStatus.RECONNECTING)
                    delay(3000)
                    currentConfig?.let { startTunnel(it) }
                } else {
                    stopTunnel(VpnStatus.DISCONNECTED)
                }
            }
        }
    }

    private fun startStatsMonitoring(config: VpnConfig) {
        statsJob = serviceScope.launch(Dispatchers.IO) {
            var prevBytesDown = 0L
            var prevBytesUp = 0L
            var detectedIp = "Fetching..."

            // Quick async IP detection
            launch {
                try {
                    val socket = Socket()
                    protect(socket)
                    socket.connect(InetSocketAddress("api.ipify.org", 80), 5000)
                    val url = URL("https://api.ipify.org")
                    val ip = url.readText().trim()
                    detectedIp = ip
                    AppLogger.i("VPN Public IP verified: $ip")
                } catch (e: Exception) {
                    detectedIp = config.serverHost
                }
            }

            while (isActive && isRunning.get()) {
                val currentDown = bytesIn.get()
                val currentUp = bytesOut.get()
                val speedDown = (currentDown - prevBytesDown).coerceAtLeast(0L)
                val speedUp = (currentUp - prevBytesUp).coerceAtLeast(0L)
                prevBytesDown = currentDown
                prevBytesUp = currentUp

                val duration = (System.currentTimeMillis() - startTimeMillis) / 1000

                // Ping calculation
                val ping = measurePing(config.serverHost, config.serverPort)

                val stats = TrafficStats(
                    bytesDown = currentDown,
                    bytesUp = currentUp,
                    speedDownBps = speedDown,
                    speedUpBps = speedUp,
                    pingMs = ping,
                    durationSeconds = duration,
                    publicIp = detectedIp
                )
                VpnController.updateStats(stats)

                // Update notification text periodically
                if (duration % 5 == 0L) {
                    val notifText = "${stats.formattedDownSpeed()} ↓ | ${stats.formattedUpSpeed()} ↑ • ${stats.formattedDuration()}"
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, buildNotification(notifText))
                }

                delay(1000)
            }
        }
    }

    private fun measurePing(host: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            protect(socket)
            socket.connect(InetSocketAddress(host, port), 2000)
            val rtt = System.currentTimeMillis() - start
            socket.close()
            rtt
        } catch (e: Exception) {
            -1L
        }
    }

    private fun stopTunnel(finalStatus: VpnStatus) {
        if (!isRunning.compareAndSet(true, false) && finalStatus != VpnStatus.ERROR) {
            VpnController.updateStatus(finalStatus)
            return
        }

        statsJob?.cancel()
        tunnelJob?.cancel()

        try {
            tunnelConnection?.close()
        } catch (_: Exception) {}
        tunnelConnection = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        VpnController.updateStatus(finalStatus)
        AppLogger.i("Dark Tunnel VPN disconnected and resources released.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTunnel(VpnStatus.DISCONNECTED)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Dark Tunnel Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live VPN connection status and metrics"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, DarkTunnelVpnService::class.java).apply {
            action = VpnController.ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Dark Tunnel VPN")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISCONNECT", disconnectPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
