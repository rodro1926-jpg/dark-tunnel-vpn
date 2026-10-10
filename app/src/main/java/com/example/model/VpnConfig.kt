package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TunnelProtocol(val displayName: String, val defaultPort: Int) {
    SSH_DIRECT("SSH (Direct)", 22),
    SSH_SSL("SSH over SSL/TLS", 443),
    SSH_HTTP_PAYLOAD("SSH + HTTP Payload", 80),
    SSH_DNSTT("SSH over DNSTT / SlowDNS", 53),
    VMESS("Xray VMess", 443),
    VLESS("Xray VLESS", 443),
    TROJAN("Trojan", 443),
    SHADOWSOCKS("Shadowsocks", 8388),
    SOCKS5("SOCKS5 Proxy", 1080),
    HYSTERIA_UDP("Hysteria / Custom UDP", 36712)
}

enum class InjectionMode(val displayName: String) {
    DIRECT("Direct"),
    DIRECT_SNI("Direct with SNI"),
    PROXY_PAYLOAD("Proxy with Payload"),
    PROXY_SNI("Proxy with SNI"),
    DNS_TUNNEL("DNS Tunnel")
}

@Entity(tableName = "vpn_configs")
data class VpnConfig(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val countryCode: String = "US",
    val countryName: String = "United States",
    val serverHost: String = "127.0.0.1",
    val serverPort: Int = 22,
    val username: String = "",
    val password: String = "",
    val protocol: TunnelProtocol = TunnelProtocol.SSH_SSL,
    val injectionMode: InjectionMode = InjectionMode.DIRECT_SNI,
    val sniHost: String = "cloudflare.com",
    val httpProxyHost: String = "",
    val httpProxyPort: Int = 8080,
    val payload: String = "",
    val uuid: String = "",
    val alterId: Int = 0,
    val security: String = "auto",
    val path: String = "/",
    val dnsttResolver: String = "8.8.8.8",
    val dnsttPublicKey: String = "",
    val dnsttNameserver: String = "",
    val customDns: String = "8.8.8.8",
    val isLocked: Boolean = false,
    val providerName: String = "",
    val providerNote: String = "",
    val expiryTimestamp: Long = 0L, // 0 = never expires
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
