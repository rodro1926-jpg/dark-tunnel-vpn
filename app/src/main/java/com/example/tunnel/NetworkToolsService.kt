package com.example.tunnel

import com.example.model.DnsQueryResult
import com.example.model.HostCheckResult
import com.example.model.IpHunterResult
import com.example.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object NetworkToolsService {

    suspend fun checkHost(
        targetUrl: String,
        requestMethod: String = "HEAD",
        customUserAgent: String = "DarkTunnel/1.0",
        proxyHost: String = "",
        proxyPort: Int = 8080
    ): HostCheckResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var urlString = targetUrl.trim()
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "https://$urlString"
        }

        try {
            val url = URL(urlString)
            val connection = if (proxyHost.isNotBlank()) {
                val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
                url.openConnection(proxy) as HttpURLConnection
            } else {
                url.openConnection() as HttpURLConnection
            }

            connection.requestMethod = requestMethod
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", customUserAgent)

            connection.connect()
            val code = connection.responseCode
            val message = connection.responseMessage ?: ""
            val rtt = System.currentTimeMillis() - start

            val headers = mutableMapOf<String, String>()
            for ((key, value) in connection.headerFields) {
                if (key != null && value.isNotEmpty()) {
                    headers[key] = value.joinToString(", ")
                }
            }

            HostCheckResult(
                url = urlString,
                statusCode = code,
                statusMessage = message,
                headers = headers,
                responseTimeMs = rtt,
                isSuccess = code in 200..399
            )
        } catch (e: Exception) {
            HostCheckResult(
                url = urlString,
                statusCode = 0,
                statusMessage = e.message ?: "Connection Failed",
                headers = emptyMap(),
                responseTimeMs = System.currentTimeMillis() - start,
                isSuccess = false
            )
        }
    }

    suspend fun huntIp(targetPrefixes: List<String>): IpHunterResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.ipify.org?format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)
            val ip = json.optString("ip", "Unknown")

            val matched = if (targetPrefixes.isEmpty()) {
                true
            } else {
                targetPrefixes.any { prefix -> ip.startsWith(prefix.trim()) }
            }

            IpHunterResult(
                currentIp = ip,
                targetPrefixes = targetPrefixes,
                isMatched = matched
            )
        } catch (e: Exception) {
            IpHunterResult(
                currentIp = "Failed to detect IP",
                targetPrefixes = targetPrefixes,
                isMatched = false
            )
        }
    }

    suspend fun pingHost(host: String, port: Int = 80, count: Int = 4): List<PingResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PingResult>()
        for (i in 1..count) {
            val start = System.currentTimeMillis()
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), 3000)
                val rtt = System.currentTimeMillis() - start
                socket.close()
                results.add(PingResult(host, i, rtt, true))
            } catch (e: Exception) {
                results.add(PingResult(host, i, -1, false, e.message))
            }
        }
        results
    }

    suspend fun resolveDns(domain: String, resolverIp: String = "8.8.8.8"): DnsQueryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val cleanDomain = domain.trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
            val addresses = InetAddress.getAllByName(cleanDomain)
            val ipList = addresses.map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
            val rtt = System.currentTimeMillis() - start

            DnsQueryResult(
                domain = cleanDomain,
                resolver = resolverIp,
                resolvedIps = ipList,
                queryTimeMs = rtt,
                isSuccess = ipList.isNotEmpty()
            )
        } catch (e: Exception) {
            DnsQueryResult(
                domain = domain,
                resolver = resolverIp,
                resolvedIps = emptyList(),
                queryTimeMs = System.currentTimeMillis() - start,
                isSuccess = false,
                error = e.message
            )
        }
    }

    suspend fun queryDnsOverHttps(domain: String): DnsQueryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val cleanDomain = domain.trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
            val url = URL("https://dns.google/resolve?name=$cleanDomain&type=A")
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/dns-json")

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)
            val answers = json.optJSONArray("Answer")
            val ips = mutableListOf<String>()
            if (answers != null) {
                for (i in 0 until answers.length()) {
                    val obj = answers.getJSONObject(i)
                    if (obj.optInt("type") == 1) { // Type A
                        ips.add(obj.optString("data"))
                    }
                }
            }
            val rtt = System.currentTimeMillis() - start

            DnsQueryResult(
                domain = cleanDomain,
                resolver = "Google DoH (https://dns.google)",
                resolvedIps = ips,
                queryTimeMs = rtt,
                isSuccess = ips.isNotEmpty()
            )
        } catch (e: Exception) {
            DnsQueryResult(
                domain = domain,
                resolver = "Google DoH",
                resolvedIps = emptyList(),
                queryTimeMs = System.currentTimeMillis() - start,
                isSuccess = false,
                error = e.message
            )
        }
    }
}
