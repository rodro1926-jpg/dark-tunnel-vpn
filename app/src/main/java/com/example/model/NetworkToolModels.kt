package com.example.model

data class HostCheckResult(
    val url: String,
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, String>,
    val responseTimeMs: Long,
    val isSuccess: Boolean
)

data class IpHunterResult(
    val currentIp: String,
    val targetPrefixes: List<String>,
    val isMatched: Boolean,
    val isp: String = "",
    val country: String = ""
)

data class PingResult(
    val host: String,
    val sequence: Int,
    val rttMs: Long,
    val isSuccess: Boolean,
    val error: String? = null
)

data class DnsQueryResult(
    val domain: String,
    val resolver: String,
    val resolvedIps: List<String>,
    val queryTimeMs: Long,
    val isSuccess: Boolean,
    val error: String? = null
)
