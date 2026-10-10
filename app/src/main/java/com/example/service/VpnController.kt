package com.example.service

import android.content.Context
import android.content.Intent
import com.example.model.TrafficStats
import com.example.model.VpnConfig
import com.example.model.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnController {
    const val ACTION_CONNECT = "com.example.darktunnel.ACTION_CONNECT"
    const val ACTION_DISCONNECT = "com.example.darktunnel.ACTION_DISCONNECT"
    const val EXTRA_CONFIG_ID = "extra_config_id"

    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    private val _activeConfig = MutableStateFlow<VpnConfig?>(null)
    val activeConfig: StateFlow<VpnConfig?> = _activeConfig.asStateFlow()

    fun updateStatus(newStatus: VpnStatus) {
        _status.value = newStatus
    }

    fun updateStats(stats: TrafficStats) {
        _trafficStats.value = stats
    }

    fun setActiveConfig(config: VpnConfig?) {
        _activeConfig.value = config
    }

    fun startVpn(context: Context, config: VpnConfig) {
        setActiveConfig(config)
        updateStatus(VpnStatus.CONNECTING)
        val intent = Intent(context, DarkTunnelVpnService::class.java).apply {
            action = ACTION_CONNECT
            putExtra(EXTRA_CONFIG_ID, config.id)
        }
        context.startService(intent)
    }

    fun stopVpn(context: Context) {
        val intent = Intent(context, DarkTunnelVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
