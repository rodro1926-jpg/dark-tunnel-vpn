package com.example.tunnel

import android.util.Base64
import com.example.model.InjectionMode
import com.example.model.TunnelProtocol
import com.example.model.VpnConfig
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object ConfigParser {
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    /**
     * Parse raw string which may be a URL scheme (vmess://, vless://, trojan://, ss://, ssh://)
     * or a raw JSON object or encrypted .dark payload.
     */
    fun parse(rawText: String, password: String? = null): Result<VpnConfig> {
        val trimmed = rawText.trim()
        return try {
            when {
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
                trimmed.startsWith("ssh://", ignoreCase = true) -> parseSsh(trimmed)
                trimmed.startsWith("{") -> parseJson(trimmed)
                trimmed.startsWith("DARKTUNNEL:") -> decryptDarkConfig(trimmed, password ?: "")
                else -> Result.failure(IllegalArgumentException("Unsupported configuration format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun base64Encode(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    private fun base64Decode(str: String): ByteArray {
        val cleaned = str.trim()
        return try {
            java.util.Base64.getDecoder().decode(cleaned)
        } catch (_: Throwable) {
            Base64.decode(cleaned, Base64.DEFAULT)
        }
    }

    private fun parseVmess(link: String): Result<VpnConfig> {
        val b64 = link.substring(8).trim()
        val decoded = String(base64Decode(b64), StandardCharsets.UTF_8)
        val json = JSONObject(decoded)
        val host = json.optString("add", "127.0.0.1")
        val port = json.optInt("port", 443)
        val uuid = json.optString("id", "")
        val aid = json.optInt("aid", 0)
        val path = json.optString("path", "/")
        val sni = json.optString("sni", json.optString("host", host))
        val remarks = json.optString("ps", "VMess Server ($host)")
        val tls = json.optString("tls", "")

        return Result.success(
            VpnConfig(
                name = remarks,
                serverHost = host,
                serverPort = port,
                protocol = TunnelProtocol.VMESS,
                injectionMode = if (tls == "tls") InjectionMode.DIRECT_SNI else InjectionMode.DIRECT,
                sniHost = sni,
                uuid = uuid,
                alterId = aid,
                path = path,
                security = json.optString("scy", "auto")
            )
        )
    }

    private fun parseVless(link: String): Result<VpnConfig> {
        val uri = URI(link)
        val uuid = uri.userInfo ?: ""
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 443
        val queryParams = parseQueryParams(uri.rawQuery ?: "")
        val sni = queryParams["sni"] ?: host
        val path = queryParams["path"] ?: "/"
        val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "VLESS ($host)"

        return Result.success(
            VpnConfig(
                name = name,
                serverHost = host,
                serverPort = port,
                protocol = TunnelProtocol.VLESS,
                injectionMode = InjectionMode.DIRECT_SNI,
                sniHost = sni,
                uuid = uuid,
                path = path
            )
        )
    }

    private fun parseTrojan(link: String): Result<VpnConfig> {
        val uri = URI(link)
        val password = uri.userInfo ?: ""
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 443
        val queryParams = parseQueryParams(uri.rawQuery ?: "")
        val sni = queryParams["sni"] ?: queryParams["peer"] ?: host
        val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Trojan ($host)"

        return Result.success(
            VpnConfig(
                name = name,
                serverHost = host,
                serverPort = port,
                protocol = TunnelProtocol.TROJAN,
                injectionMode = InjectionMode.DIRECT_SNI,
                sniHost = sni,
                password = password
            )
        )
    }

    private fun parseShadowsocks(link: String): Result<VpnConfig> {
        val withoutScheme = link.substring(5)
        val fragmentIndex = withoutScheme.indexOf('#')
        val name = if (fragmentIndex != -1) {
            URLDecoder.decode(withoutScheme.substring(fragmentIndex + 1), "UTF-8")
        } else {
            "Shadowsocks Server"
        }
        val mainPart = if (fragmentIndex != -1) withoutScheme.substring(0, fragmentIndex) else withoutScheme

        var host = "127.0.0.1"
        var port = 8388
        var password = ""

        if (mainPart.contains("@")) {
            val parts = mainPart.split("@")
            val userInfo = parts[0]
            val decodedUserInfo = try {
                String(base64Decode(userInfo), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                userInfo
            }
            password = decodedUserInfo
            val hostPort = parts[1].split(":")
            host = hostPort[0]
            port = hostPort.getOrNull(1)?.toIntOrNull() ?: 8388
        }

        return Result.success(
            VpnConfig(
                name = name,
                serverHost = host,
                serverPort = port,
                protocol = TunnelProtocol.SHADOWSOCKS,
                injectionMode = InjectionMode.DIRECT,
                password = password
            )
        )
    }

    private fun parseSsh(link: String): Result<VpnConfig> {
        val uri = URI(link)
        val userInfo = uri.userInfo ?: ""
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 22
        val userParts = userInfo.split(":")
        val username = userParts.getOrNull(0) ?: "root"
        val password = userParts.getOrNull(1) ?: ""
        val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "SSH ($host)"

        return Result.success(
            VpnConfig(
                name = name,
                serverHost = host,
                serverPort = port,
                username = username,
                password = password,
                protocol = TunnelProtocol.SSH_DIRECT,
                injectionMode = InjectionMode.DIRECT
            )
        )
    }

    fun toJson(config: VpnConfig): String {
        val json = JSONObject()
        json.put("name", config.name)
        json.put("countryCode", config.countryCode)
        json.put("countryName", config.countryName)
        json.put("serverHost", config.serverHost)
        json.put("serverPort", config.serverPort)
        json.put("username", config.username)
        json.put("password", config.password)
        json.put("protocol", config.protocol.name)
        json.put("injectionMode", config.injectionMode.name)
        json.put("sniHost", config.sniHost)
        json.put("httpProxyHost", config.httpProxyHost)
        json.put("httpProxyPort", config.httpProxyPort)
        json.put("payload", config.payload)
        json.put("uuid", config.uuid)
        json.put("alterId", config.alterId)
        json.put("path", config.path)
        json.put("dnsttResolver", config.dnsttResolver)
        json.put("dnsttPublicKey", config.dnsttPublicKey)
        json.put("dnsttNameserver", config.dnsttNameserver)
        json.put("customDns", config.customDns)
        json.put("isLocked", config.isLocked)
        json.put("providerName", config.providerName)
        json.put("providerNote", config.providerNote)
        json.put("expiryTimestamp", config.expiryTimestamp)
        return json.toString(2)
    }

    private fun parseJson(jsonString: String): Result<VpnConfig> {
        val json = JSONObject(jsonString)
        val protocolName = json.optString("protocol", TunnelProtocol.SSH_SSL.name)
        val protocol = try {
            TunnelProtocol.valueOf(protocolName)
        } catch (e: Exception) {
            TunnelProtocol.SSH_SSL
        }
        val injectName = json.optString("injectionMode", InjectionMode.DIRECT_SNI.name)
        val injectMode = try {
            InjectionMode.valueOf(injectName)
        } catch (e: Exception) {
            InjectionMode.DIRECT_SNI
        }

        return Result.success(
            VpnConfig(
                name = json.optString("name", "Imported Configuration"),
                countryCode = json.optString("countryCode", "US"),
                countryName = json.optString("countryName", "Custom"),
                serverHost = json.optString("serverHost", "127.0.0.1"),
                serverPort = json.optInt("serverPort", 443),
                username = json.optString("username", ""),
                password = json.optString("password", ""),
                protocol = protocol,
                injectionMode = injectMode,
                sniHost = json.optString("sniHost", ""),
                httpProxyHost = json.optString("httpProxyHost", ""),
                httpProxyPort = json.optInt("httpProxyPort", 8080),
                payload = json.optString("payload", ""),
                uuid = json.optString("uuid", ""),
                alterId = json.optInt("alterId", 0),
                path = json.optString("path", "/"),
                dnsttResolver = json.optString("dnsttResolver", "8.8.8.8"),
                dnsttPublicKey = json.optString("dnsttPublicKey", ""),
                dnsttNameserver = json.optString("dnsttNameserver", ""),
                customDns = json.optString("customDns", "8.8.8.8"),
                isLocked = json.optBoolean("isLocked", false),
                providerName = json.optString("providerName", ""),
                providerNote = json.optString("providerNote", ""),
                expiryTimestamp = json.optLong("expiryTimestamp", 0L)
            )
        )
    }

    /**
     * Encrypt a VpnConfig into a locked DARKTUNNEL file with optional password
     */
    fun encryptDarkConfig(config: VpnConfig, password: String): String {
        val jsonBytes = toJson(config).toByteArray(StandardCharsets.UTF_8)
        val salt = ByteArray(16)
        val iv = ByteArray(GCM_IV_LENGTH)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(jsonBytes)

        val output = ByteArray(salt.size + iv.size + encrypted.size)
        System.arraycopy(salt, 0, output, 0, salt.size)
        System.arraycopy(iv, 0, output, salt.size, iv.size)
        System.arraycopy(encrypted, 0, output, salt.size + iv.size, encrypted.size)

        val b64 = base64Encode(output)
        return "DARKTUNNEL:$b64"
    }

    private fun decryptDarkConfig(darkString: String, password: String): Result<VpnConfig> {
        val b64 = darkString.substringAfter("DARKTUNNEL:").trim()
        val data = base64Decode(b64)
        if (data.size < 16 + GCM_IV_LENGTH + 16) {
            return Result.failure(IllegalArgumentException("Corrupt encrypted package"))
        }

        val salt = ByteArray(16)
        val iv = ByteArray(GCM_IV_LENGTH)
        val encrypted = ByteArray(data.size - 16 - GCM_IV_LENGTH)

        System.arraycopy(data, 0, salt, 0, 16)
        System.arraycopy(data, 16, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(data, 16 + GCM_IV_LENGTH, encrypted, 0, encrypted.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val decryptedBytes = cipher.doFinal(encrypted)
        val jsonStr = String(decryptedBytes, StandardCharsets.UTF_8)
        return parseJson(jsonStr)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        query.split("&").forEach { param ->
            val parts = param.split("=")
            if (parts.size == 2) {
                map[parts[0]] = URLDecoder.decode(parts[1], "UTF-8")
            }
        }
        return map
    }

    fun validateConfig(config: VpnConfig): String? {
        if (config.serverHost.isBlank()) {
            return "Server host/IP cannot be empty."
        }
        if (config.serverPort !in 1..65535) {
            return "Invalid server port: ${config.serverPort}."
        }
        if (config.expiryTimestamp > 0 && System.currentTimeMillis() > config.expiryTimestamp) {
            return "This configuration expired on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(config.expiryTimestamp))}."
        }
        return null
    }
}
