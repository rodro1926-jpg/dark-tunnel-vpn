package com.example.data

import com.example.model.InjectionMode
import com.example.model.PayloadInjectMethod
import com.example.model.PayloadRequestMethod
import com.example.model.PayloadTemplate
import com.example.model.TunnelProtocol
import com.example.model.VpnConfig

object DefaultServers {
    val INITIAL_CONFIGS = listOf(
        VpnConfig(
            id = "server-ssh-direct-01",
            name = "SSH Direct + Payload (Custom)",
            countryCode = "GLOBAL",
            countryName = "Custom Server",
            serverHost = "127.0.0.1",
            serverPort = 22,
            username = "root",
            password = "",
            protocol = TunnelProtocol.SSH_DIRECT,
            injectionMode = InjectionMode.DIRECT,
            payload = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Keep-Alive[crlf]User-Agent: [ua][crlf][crlf]",
            isFavorite = true
        ),
        VpnConfig(
            id = "server-bd-01",
            name = "Bangladesh Premium (BD-01)",
            countryCode = "BD",
            countryName = "Bangladesh",
            serverHost = "bd01.darktunnel.net",
            serverPort = 443,
            username = "dark_user",
            password = "",
            protocol = TunnelProtocol.SSH_SSL,
            injectionMode = InjectionMode.DIRECT_SNI,
            sniHost = "cloudflare.com",
            isFavorite = true
        ),
        VpnConfig(
            id = "server-sg-01",
            name = "Singapore Ultra Low Ping (SG-01)",
            countryCode = "SG",
            countryName = "Singapore",
            serverHost = "sg01.darktunnel.net",
            serverPort = 443,
            username = "dark_user",
            password = "",
            protocol = TunnelProtocol.VMESS,
            injectionMode = InjectionMode.DIRECT_SNI,
            sniHost = "sg01.darktunnel.net",
            uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            path = "/vmess-ws",
            isFavorite = true
        ),
        VpnConfig(
            id = "server-in-01",
            name = "India High Speed (IN-01)",
            countryCode = "IN",
            countryName = "India",
            serverHost = "in01.darktunnel.net",
            serverPort = 80,
            username = "dark_user",
            password = "",
            protocol = TunnelProtocol.SSH_HTTP_PAYLOAD,
            injectionMode = InjectionMode.PROXY_PAYLOAD,
            httpProxyHost = "104.16.248.249",
            httpProxyPort = 80,
            payload = "CONNECT [host_port] [protocol][crlf]Host: [host][crlf]User-Agent: [ua][crlf]Connection: Keep-Alive[crlf][crlf]"
        ),
        VpnConfig(
            id = "server-us-01",
            name = "USA Streaming & Gaming (US-01)",
            countryCode = "US",
            countryName = "United States",
            serverHost = "us01.darktunnel.net",
            serverPort = 443,
            username = "dark_user",
            password = "trojan_secret_token",
            protocol = TunnelProtocol.TROJAN,
            injectionMode = InjectionMode.DIRECT_SNI,
            sniHost = "us01.darktunnel.net"
        ),
        VpnConfig(
            id = "server-dns-01",
            name = "SlowDNS / DNSTT Tunnel (Global)",
            countryCode = "GL",
            countryName = "Global DNSTT",
            serverHost = "127.0.0.1",
            serverPort = 22,
            username = "dnstt_user",
            protocol = TunnelProtocol.SSH_DNSTT,
            injectionMode = InjectionMode.DNS_TUNNEL,
            dnsttResolver = "8.8.8.8",
            dnsttNameserver = "ns.darktunnel.net",
            dnsttPublicKey = "0000000000000000000000000000000000000000000000000000000000000000"
        )
    )

    val DEFAULT_PAYLOADS = listOf(
        PayloadTemplate(
            id = "payload-cf-ws",
            name = "Cloudflare CDN WebSocket",
            description = "CONNECT with Upgrade WebSocket & Host header",
            payloadString = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Keep-Alive[crlf]User-Agent: [ua][crlf][crlf]",
            injectMethod = PayloadInjectMethod.NORMAL,
            requestMethod = PayloadRequestMethod.GET,
            isCustom = false
        ),
        PayloadTemplate(
            id = "payload-direct-connect",
            name = "Direct HTTP CONNECT",
            description = "Standard HTTP CONNECT proxy tunnel request",
            payloadString = "CONNECT [host_port] [protocol][crlf]Host: [host][crlf]X-Online-Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]",
            injectMethod = PayloadInjectMethod.NORMAL,
            requestMethod = PayloadRequestMethod.CONNECT,
            isCustom = false
        ),
        PayloadTemplate(
            id = "payload-split-front",
            name = "Front Inject Split",
            description = "Front injected payload split with [split] delimiter",
            payloadString = "GET http://[host]/ [protocol][crlf]Host: [host][crlf][split]CONNECT [host_port] [protocol][crlf][crlf]",
            injectMethod = PayloadInjectMethod.SPLIT,
            requestMethod = PayloadRequestMethod.GET,
            isCustom = false
        )
    )
}
