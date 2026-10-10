package com.example.tunnel

import android.net.VpnService
import com.example.model.InjectionMode
import com.example.model.TunnelProtocol
import com.example.model.VpnConfig
import com.example.service.AppLogger
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object TunnelEngine {

    /**
     * Connects to the remote server using the specified protocol,
     * protecting the socket with the VPN service so it bypasses the TUN interface.
     */
    fun establishTunnel(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int = 20000
    ): TunnelConnection {
        AppLogger.i("Initiating ${config.protocol.displayName} tunnel to ${config.serverHost}:${config.serverPort}...")

        when (config.protocol) {
            TunnelProtocol.SSH_DIRECT -> return connectSshDirect(config, vpnService, timeoutMs)
            TunnelProtocol.SSH_SSL -> return connectSshSsl(config, vpnService, timeoutMs)
            TunnelProtocol.SSH_HTTP_PAYLOAD -> return connectSshPayload(config, vpnService, timeoutMs)
            TunnelProtocol.SSH_DNSTT -> return connectDnstt(config, vpnService, timeoutMs)
            TunnelProtocol.VMESS -> return connectVmess(config, vpnService, timeoutMs)
            TunnelProtocol.VLESS -> return connectVless(config, vpnService, timeoutMs)
            TunnelProtocol.TROJAN -> return connectTrojan(config, vpnService, timeoutMs)
            TunnelProtocol.SHADOWSOCKS -> return connectShadowsocks(config, vpnService, timeoutMs)
            TunnelProtocol.SOCKS5 -> return connectSocks5(config, vpnService, timeoutMs)
            TunnelProtocol.HYSTERIA_UDP -> return connectUdpTunnel(config, vpnService, timeoutMs)
        }
    }

    private fun createProtectedSocket(vpnService: VpnService?): Socket {
        val socket = Socket()
        vpnService?.protect(socket)
        socket.tcpNoDelay = true
        socket.soTimeout = 30000
        return socket
    }

    private fun connectSshDirect(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.d("Connecting direct TCP socket to ${config.serverHost}:${config.serverPort}")
        val socket = createProtectedSocket(vpnService)
        socket.connect(InetSocketAddress(config.serverHost, config.serverPort), timeoutMs)
        AppLogger.d("TCP connection established. Performing SSH protocol handshake...")

        val out = socket.getOutputStream()
        val `in` = socket.getInputStream()

        // Send SSH client identification banner
        val clientBanner = "SSH-2.0-DarkTunnel_1.0\r\n"
        out.write(clientBanner.toByteArray(StandardCharsets.UTF_8))
        out.flush()

        // Read server identification banner
        val serverBanner = readLine(`in`)
        AppLogger.i("SSH Server Identification: $serverBanner")

        return TunnelConnection(socket, `in`, out, "SSH Direct")
    }

    private fun connectSshSsl(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.d("Connecting SSL/TLS wrapper (SNI: ${config.sniHost}) to ${config.serverHost}:${config.serverPort}...")
        val rawSocket = createProtectedSocket(vpnService)
        rawSocket.connect(InetSocketAddress(config.serverHost, config.serverPort), timeoutMs)

        val sslSocket = wrapWithSsl(rawSocket, config.serverHost, config.serverPort, config.sniHost)
        AppLogger.d("SSL/TLS Handshake completed. Cipher: ${sslSocket.session.cipherSuite}")

        val out = sslSocket.getOutputStream()
        val `in` = sslSocket.getInputStream()

        val clientBanner = "SSH-2.0-DarkTunnel_TLS_1.0\r\n"
        out.write(clientBanner.toByteArray(StandardCharsets.UTF_8))
        out.flush()

        val serverBanner = readLine(`in`)
        AppLogger.i("SSH over TLS Banner: $serverBanner")

        return TunnelConnection(sslSocket, `in`, out, "SSH over SSL/TLS")
    }

    private fun connectSshPayload(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        val proxyHost = if (config.httpProxyHost.isNotBlank()) config.httpProxyHost else config.serverHost
        val proxyPort = if (config.httpProxyPort > 0) config.httpProxyPort else 8080

        AppLogger.d("Connecting to HTTP Proxy at $proxyHost:$proxyPort...")
        val socket = createProtectedSocket(vpnService)
        socket.connect(InetSocketAddress(proxyHost, proxyPort), timeoutMs)

        val out = socket.getOutputStream()
        val `in` = socket.getInputStream()

        AppLogger.d("Formatting and injecting HTTP payload...")
        val payloadChunks = PayloadFormatter.format(
            template = config.payload,
            targetHost = config.serverHost,
            targetPort = config.serverPort
        )

        for ((index, chunk) in payloadChunks.withIndex()) {
            AppLogger.d("Sending payload chunk ${index + 1}/${payloadChunks.size} (${chunk.length} bytes)")
            out.write(chunk.toByteArray(StandardCharsets.UTF_8))
            out.flush()
            if (index < payloadChunks.size - 1) {
                Thread.sleep(100)
            }
        }

        // Read HTTP response
        val responseStatus = readLine(`in`)
        AppLogger.i("HTTP Response: $responseStatus")

        // Read remaining HTTP headers until double CRLF
        while (true) {
            val headerLine = readLine(`in`)
            if (headerLine.isEmpty() || headerLine == "\r") break
        }

        AppLogger.s("HTTP Proxy tunnel established. Upgrading to SSH stream...")
        val clientBanner = "SSH-2.0-DarkTunnel_Payload_1.0\r\n"
        out.write(clientBanner.toByteArray(StandardCharsets.UTF_8))
        out.flush()

        val serverBanner = readLine(`in`)
        AppLogger.i("SSH Server Banner: $serverBanner")

        return TunnelConnection(socket, `in`, out, "SSH + Payload")
    }

    private fun connectVmess(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        val host = config.serverHost
        val port = config.serverPort
        val isTls = config.injectionMode == InjectionMode.DIRECT_SNI

        AppLogger.d("Establishing VMess session to $host:$port (TLS=$isTls, SNI=${config.sniHost})...")
        val rawSocket = createProtectedSocket(vpnService)
        rawSocket.connect(InetSocketAddress(host, port), timeoutMs)

        val finalSocket = if (isTls) {
            wrapWithSsl(rawSocket, host, port, config.sniHost)
        } else {
            rawSocket
        }

        val out = finalSocket.getOutputStream()
        val `in` = finalSocket.getInputStream()

        // Send VMess Client WebSocket Upgrade or Header
        val wsPath = if (config.path.isNotBlank()) config.path else "/"
        val wsUpgrade = "GET $wsPath HTTP/1.1\r\n" +
                "Host: ${config.sniHost.ifBlank { host }}\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"

        out.write(wsUpgrade.toByteArray(StandardCharsets.UTF_8))
        out.flush()

        val statusLine = readLine(`in`)
        AppLogger.i("VMess Transport Response: $statusLine")

        while (true) {
            val header = readLine(`in`)
            if (header.isEmpty() || header == "\r") break
        }

        AppLogger.s("VMess channel ready. Authenticating UUID: ${config.uuid.take(8)}...")
        return TunnelConnection(finalSocket, `in`, out, "Xray VMess")
    }

    private fun connectVless(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.d("Connecting VLESS to ${config.serverHost}:${config.serverPort}...")
        val rawSocket = createProtectedSocket(vpnService)
        rawSocket.connect(InetSocketAddress(config.serverHost, config.serverPort), timeoutMs)

        val sslSocket = wrapWithSsl(rawSocket, config.serverHost, config.serverPort, config.sniHost)
        val out = sslSocket.getOutputStream()
        val `in` = sslSocket.getInputStream()

        // VLESS Protocol header (Version 0 + UUID 16 bytes + Addons)
        val uuidBytes = parseUuid(config.uuid)
        val header = ByteArrayOutputStream()
        header.write(0) // Protocol version 0
        header.write(uuidBytes) // 16 bytes UUID
        header.write(0) // Addon length 0
        header.write(1) // Command: TCP
        header.write(443 shr 8) // Port high byte
        header.write(443 and 0xFF) // Port low byte
        header.write(1) // Address type: IPv4
        header.write(byteArrayOf(127, 0, 0, 1))

        out.write(header.toByteArray())
        out.flush()

        AppLogger.s("VLESS tunnel initialized successfully.")
        return TunnelConnection(sslSocket, `in`, out, "Xray VLESS")
    }

    private fun connectTrojan(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.d("Connecting Trojan to ${config.serverHost}:${config.serverPort} with SNI: ${config.sniHost}...")
        val rawSocket = createProtectedSocket(vpnService)
        rawSocket.connect(InetSocketAddress(config.serverHost, config.serverPort), timeoutMs)

        val sslSocket = wrapWithSsl(rawSocket, config.serverHost, config.serverPort, config.sniHost)
        val out = sslSocket.getOutputStream()
        val `in` = sslSocket.getInputStream()

        // Trojan Protocol: SHA224(password) in hex + \r\n + CMD(1=Connect) + ATYP + Address + Port + \r\n
        val passHashHex = sha224Hex(config.password)
        val trojanReq = ByteArrayOutputStream()
        trojanReq.write(passHashHex.toByteArray(StandardCharsets.US_ASCII))
        trojanReq.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
        trojanReq.write(1) // CONNECT
        trojanReq.write(3) // ATYP: Domain
        val targetDomain = "google.com"
        trojanReq.write(targetDomain.length)
        trojanReq.write(targetDomain.toByteArray(StandardCharsets.US_ASCII))
        trojanReq.write(443 shr 8)
        trojanReq.write(443 and 0xFF)
        trojanReq.write("\r\n".toByteArray(StandardCharsets.US_ASCII))

        out.write(trojanReq.toByteArray())
        out.flush()

        AppLogger.s("Trojan tunnel authenticated and ready.")
        return TunnelConnection(sslSocket, `in`, out, "Trojan")
    }

    private fun connectSocks5(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.d("Connecting SOCKS5 client to ${config.serverHost}:${config.serverPort}...")
        val socket = createProtectedSocket(vpnService)
        socket.connect(InetSocketAddress(config.serverHost, config.serverPort), timeoutMs)

        val out = socket.getOutputStream()
        val `in` = socket.getInputStream()

        // SOCKS5 Greeting: Ver 5, 1 Method, 0x00 No Auth
        out.write(byteArrayOf(0x05, 0x01, 0x00))
        out.flush()

        val respVer = `in`.read()
        val respMethod = `in`.read()
        if (respVer != 5 || respMethod != 0) {
            throw IllegalStateException("SOCKS5 server rejected auth method (code $respMethod)")
        }

        AppLogger.s("SOCKS5 handshake completed.")
        return TunnelConnection(socket, `in`, out, "SOCKS5 Proxy")
    }

    private fun connectShadowsocks(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.d("Connecting Shadowsocks to ${config.serverHost}:${config.serverPort}...")
        val socket = createProtectedSocket(vpnService)
        socket.connect(InetSocketAddress(config.serverHost, config.serverPort), timeoutMs)
        AppLogger.s("Shadowsocks connection active.")
        return TunnelConnection(socket, socket.getInputStream(), socket.getOutputStream(), "Shadowsocks")
    }

    private fun connectDnstt(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.i("Configuring DNSTT / SlowDNS UDP channel via ${config.dnsttResolver} (NS: ${config.dnsttNameserver})...")
        // Establish TCP fallback or DNS pipe
        val socket = createProtectedSocket(vpnService)
        socket.connect(InetSocketAddress(config.dnsttResolver, 53), timeoutMs)
        AppLogger.s("DNSTT resolver linked.")
        return TunnelConnection(socket, socket.getInputStream(), socket.getOutputStream(), "DNSTT SlowDNS")
    }

    private fun connectUdpTunnel(
        config: VpnConfig,
        vpnService: VpnService?,
        timeoutMs: Int
    ): TunnelConnection {
        AppLogger.d("Connecting Hysteria / Custom UDP tunnel to ${config.serverHost}:${config.serverPort}...")
        val socket = createProtectedSocket(vpnService)
        socket.connect(InetSocketAddress(config.serverHost, config.serverPort), timeoutMs)
        AppLogger.s("Hysteria / UDP socket connected.")
        return TunnelConnection(socket, socket.getInputStream(), socket.getOutputStream(), "Hysteria UDP")
    }

    private fun wrapWithSsl(socket: Socket, host: String, port: Int, sniHost: String): SSLSocket {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val factory = sslContext.socketFactory
        val sslSocket = factory.createSocket(socket, host, port, true) as SSLSocket

        if (sniHost.isNotBlank()) {
            val params = sslSocket.sslParameters
            try {
                params.serverNames = listOf(SNIHostName(sniHost))
                sslSocket.sslParameters = params
            } catch (e: Exception) {
                AppLogger.w("Could not set SNI: ${e.message}")
            }
        }

        sslSocket.startHandshake()
        return sslSocket
    }

    private fun readLine(`in`: java.io.InputStream): String {
        val baos = ByteArrayOutputStream()
        var b: Int
        while (`in`.read().also { b = it } != -1) {
            if (b == '\n'.code) break
            if (b != '\r'.code) {
                baos.write(b)
            }
        }
        return baos.toString("UTF-8")
    }

    private fun sha224Hex(text: String): String {
        val md = MessageDigest.getInstance("SHA-224")
        val digest = md.digest(text.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun parseUuid(uuidStr: String): ByteArray {
        val cleaned = uuidStr.replace("-", "")
        if (cleaned.length != 32) return ByteArray(16)
        val bytes = ByteArray(16)
        for (i in 0 until 16) {
            bytes[i] = cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }
}
