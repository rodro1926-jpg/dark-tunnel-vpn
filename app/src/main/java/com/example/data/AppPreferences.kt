package com.example.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dark_tunnel_prefs", Context.MODE_PRIVATE)

    var autoReconnect: Boolean
        get() = prefs.getBoolean("auto_reconnect", true)
        set(value) = prefs.edit().putBoolean("auto_reconnect", value).apply()

    var connectionTimeoutSeconds: Int
        get() = prefs.getInt("connection_timeout", 30)
        set(value) = prefs.edit().putInt("connection_timeout", value).apply()

    var keepAliveIntervalSeconds: Int
        get() = prefs.getInt("keep_alive_interval", 15)
        set(value) = prefs.edit().putInt("keep_alive_interval", value).apply()

    var mtu: Int
        get() = prefs.getInt("mtu", 1500)
        set(value) = prefs.edit().putInt("mtu", value).apply()

    var enableIpv6: Boolean
        get() = prefs.getBoolean("enable_ipv6", false)
        set(value) = prefs.edit().putBoolean("enable_ipv6", value).apply()

    var enableKillSwitch: Boolean
        get() = prefs.getBoolean("enable_kill_switch", false)
        set(value) = prefs.edit().putBoolean("enable_kill_switch", value).apply()

    var wakeLockEnabled: Boolean
        get() = prefs.getBoolean("wake_lock", true)
        set(value) = prefs.edit().putBoolean("wake_lock", value).apply()

    var selectedDns: String
        get() = prefs.getString("selected_dns", "8.8.8.8") ?: "8.8.8.8"
        set(value) = prefs.edit().putString("selected_dns", value).apply()

    var selectedDnsName: String
        get() = prefs.getString("selected_dns_name", "Google DNS (8.8.8.8)") ?: "Google DNS (8.8.8.8)"
        set(value) = prefs.edit().putString("selected_dns_name", value).apply()

    var customDns: String
        get() = prefs.getString("custom_dns", "1.1.1.1") ?: "1.1.1.1"
        set(value) = prefs.edit().putString("custom_dns", value).apply()

    var perAppVpnEnabled: Boolean
        get() = prefs.getBoolean("per_app_vpn_enabled", false)
        set(value) = prefs.edit().putBoolean("per_app_vpn_enabled", value).apply()

    var perAppExcludeMode: Boolean // true: exclude selected apps, false: only include selected apps
        get() = prefs.getBoolean("per_app_exclude_mode", true)
        set(value) = prefs.edit().putBoolean("per_app_exclude_mode", value).apply()

    var selectedApps: Set<String>
        get() = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("selected_apps", value).apply()

    var activeConfigId: String?
        get() = prefs.getString("active_config_id", null)
        set(value) = prefs.edit().putString("active_config_id", value).apply()

    var providerBrandName: String
        get() = prefs.getString("provider_brand_name", "Dark Tunnel Official") ?: "Dark Tunnel Official"
        set(value) = prefs.edit().putString("provider_brand_name", value).apply()

    var providerMessage: String
        get() = prefs.getString("provider_message", "Welcome to Dark Tunnel Secure VPN") ?: "Welcome to Dark Tunnel Secure VPN"
        set(value) = prefs.edit().putString("provider_message", value).apply()
}
