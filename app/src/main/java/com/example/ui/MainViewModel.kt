package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.VpnDatabase
import com.example.model.DnsQueryResult
import com.example.model.HostCheckResult
import com.example.model.IpHunterResult
import com.example.model.LogEntry
import com.example.model.PayloadTemplate
import com.example.model.PingResult
import com.example.model.TrafficStats
import com.example.model.VpnConfig
import com.example.model.VpnStatus
import com.example.service.AppLogger
import com.example.service.VpnController
import com.example.tunnel.ConfigParser
import com.example.tunnel.NetworkToolsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = VpnDatabase.getDatabase(application)
    val preferences = AppPreferences(application)

    val vpnStatus: StateFlow<VpnStatus> = VpnController.status
    val trafficStats: StateFlow<TrafficStats> = VpnController.trafficStats
    val activeConfig: StateFlow<VpnConfig?> = VpnController.activeConfig

    val configs: StateFlow<List<VpnConfig>> = database.vpnConfigDao()
        .getAllConfigs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val payloads: StateFlow<List<PayloadTemplate>> = database.payloadDao()
        .getAllPayloads()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val logs: StateFlow<List<LogEntry>> = AppLogger.logsFlow

    // Tool states
    private val _hostCheckResult = MutableStateFlow<HostCheckResult?>(null)
    val hostCheckResult: StateFlow<HostCheckResult?> = _hostCheckResult.asStateFlow()

    private val _isHostChecking = MutableStateFlow(false)
    val isHostChecking: StateFlow<Boolean> = _isHostChecking.asStateFlow()

    private val _ipHunterResult = MutableStateFlow<IpHunterResult?>(null)
    val ipHunterResult: StateFlow<IpHunterResult?> = _ipHunterResult.asStateFlow()

    private val _isHuntingIp = MutableStateFlow(false)
    val isHuntingIp: StateFlow<Boolean> = _isHuntingIp.asStateFlow()

    private val _pingResults = MutableStateFlow<List<PingResult>>(emptyList())
    val pingResults: StateFlow<List<PingResult>> = _pingResults.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _dnsResult = MutableStateFlow<DnsQueryResult?>(null)
    val dnsResult: StateFlow<DnsQueryResult?> = _dnsResult.asStateFlow()

    private val _isQueryingDns = MutableStateFlow(false)
    val isQueryingDns: StateFlow<Boolean> = _isQueryingDns.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    init {
        // Restore active config or pick first, and seed SSH direct profile if missing
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = database.vpnConfigDao().getAllConfigsSync()
                if (existing.none { it.protocol == com.example.model.TunnelProtocol.SSH_DIRECT }) {
                    com.example.data.DefaultServers.INITIAL_CONFIGS.firstOrNull { it.protocol == com.example.model.TunnelProtocol.SSH_DIRECT }?.let {
                        database.vpnConfigDao().insertConfig(it)
                    }
                }
            }
            configs.collect { list ->
                if (list.isNotEmpty() && VpnController.activeConfig.value == null) {
                    val savedId = preferences.activeConfigId
                    val chosen = list.find { it.id == savedId } ?: list.first()
                    VpnController.setActiveConfig(chosen)
                }
            }
        }
    }

    fun updateActiveConfigPayload(newPayload: String) {
        val current = activeConfig.value ?: return
        val updated = current.copy(payload = newPayload)
        saveConfig(updated)
    }

    fun updateActiveConfigServer(host: String, port: Int, user: String, pass: String) {
        val current = activeConfig.value ?: return
        val updated = current.copy(
            serverHost = host,
            serverPort = port,
            username = user,
            password = pass
        )
        saveConfig(updated)
    }

    fun selectConfig(config: VpnConfig) {
        preferences.activeConfigId = config.id
        VpnController.setActiveConfig(config)
        AppLogger.i("Selected profile: ${config.name} (${config.protocol.displayName})")
    }

    fun toggleVpn(context: Context) {
        val currentStatus = vpnStatus.value
        if (currentStatus == VpnStatus.DISCONNECTED || currentStatus == VpnStatus.ERROR) {
            val config = activeConfig.value ?: configs.value.firstOrNull()
            if (config != null) {
                VpnController.startVpn(context, config)
            } else {
                AppLogger.e("Cannot start: No VPN configuration available!")
            }
        } else {
            VpnController.stopVpn(context)
        }
    }

    fun saveConfig(config: VpnConfig) {
        viewModelScope.launch {
            database.vpnConfigDao().insertConfig(config)
            if (VpnController.activeConfig.value?.id == config.id) {
                VpnController.setActiveConfig(config)
            }
            AppLogger.s("Configuration [${config.name}] saved successfully.")
        }
    }

    fun duplicateConfig(config: VpnConfig) {
        viewModelScope.launch {
            val duplicate = config.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${config.name} (Copy)",
                createdAt = System.currentTimeMillis()
            )
            database.vpnConfigDao().insertConfig(duplicate)
            AppLogger.i("Duplicated configuration as [${duplicate.name}].")
        }
    }

    fun deleteConfig(config: VpnConfig) {
        viewModelScope.launch {
            database.vpnConfigDao().deleteConfig(config)
            if (VpnController.activeConfig.value?.id == config.id) {
                val remaining = configs.value.filter { it.id != config.id }
                VpnController.setActiveConfig(remaining.firstOrNull())
            }
            AppLogger.i("Deleted configuration [${config.name}].")
        }
    }

    fun importConfig(rawText: String, password: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = ConfigParser.parse(rawText, password)
            if (result.isSuccess) {
                val cfg = result.getOrThrow()
                database.vpnConfigDao().insertConfig(cfg)
                selectConfig(cfg)
                AppLogger.s("Successfully imported [${cfg.name}]!")
                onResult(true, "Config '${cfg.name}' imported successfully!")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown import error"
                AppLogger.e("Import failed: $errorMsg")
                onResult(false, errorMsg)
            }
        }
    }

    fun savePayload(payload: PayloadTemplate) {
        viewModelScope.launch {
            database.payloadDao().insertPayload(payload)
            AppLogger.s("Payload template [${payload.name}] saved.")
        }
    }

    fun deletePayload(payload: PayloadTemplate) {
        viewModelScope.launch {
            database.payloadDao().deletePayload(payload)
            AppLogger.i("Payload [${payload.name}] removed.")
        }
    }

    fun runHostCheck(targetUrl: String, method: String, ua: String, proxyHost: String, proxyPort: Int) {
        viewModelScope.launch {
            _isHostChecking.value = true
            _hostCheckResult.value = null
            val res = NetworkToolsService.checkHost(targetUrl, method, ua, proxyHost, proxyPort)
            _hostCheckResult.value = res
            _isHostChecking.value = false
            AppLogger.i("Host check completed for $targetUrl: HTTP ${res.statusCode} (${res.responseTimeMs}ms)")
        }
    }

    fun runIpHunter(targetPrefixes: List<String>) {
        viewModelScope.launch {
            _isHuntingIp.value = true
            _ipHunterResult.value = null
            val res = NetworkToolsService.huntIp(targetPrefixes)
            _ipHunterResult.value = res
            _isHuntingIp.value = false
            AppLogger.i("IP Hunter result: ${res.currentIp} (Matched: ${res.isMatched})")
        }
    }

    fun runPing(host: String, port: Int = 80) {
        viewModelScope.launch {
            _isPinging.value = true
            _pingResults.value = emptyList()
            val list = NetworkToolsService.pingHost(host, port)
            _pingResults.value = list
            _isPinging.value = false
            val avg = if (list.isNotEmpty()) list.map { it.rttMs }.filter { it > 0 }.average().toLong() else 0L
            AppLogger.i("Ping test to $host complete. Average RTT: ${avg}ms")
        }
    }

    fun runDnsQuery(domain: String, resolver: String) {
        viewModelScope.launch {
            _isQueryingDns.value = true
            _dnsResult.value = null
            val res = NetworkToolsService.resolveDns(domain, resolver)
            _dnsResult.value = res
            _isQueryingDns.value = false
        }
    }

    fun runDohQuery(domain: String) {
        viewModelScope.launch {
            _isQueryingDns.value = true
            _dnsResult.value = null
            val res = NetworkToolsService.queryDnsOverHttps(domain)
            _dnsResult.value = res
            _isQueryingDns.value = false
        }
    }

    fun loadInstalledApps() {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                packages.map { info ->
                    InstalledAppItem(
                        name = info.loadLabel(pm).toString(),
                        packageName = info.packageName,
                        isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }.sortedBy { it.name.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun toggleAppFilter(packageName: String) {
        val current = preferences.selectedApps.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        preferences.selectedApps = current
    }

    fun clearLogs() {
        AppLogger.clear()
    }
}
