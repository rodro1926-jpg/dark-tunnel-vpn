package com.example.tunnel

object PayloadFormatter {
    private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    fun format(
        template: String,
        targetHost: String,
        targetPort: Int,
        protocol: String = "HTTP/1.1",
        customUserAgent: String = DEFAULT_UA
    ): List<String> {
        if (template.isBlank()) {
            return listOf("CONNECT $targetHost:$targetPort $protocol\r\nHost: $targetHost\r\nConnection: Keep-Alive\r\n\r\n")
        }

        var processed = template
            .replace("[host_port]", "$targetHost:$targetPort", ignoreCase = true)
            .replace("[host]", targetHost, ignoreCase = true)
            .replace("[port]", targetPort.toString(), ignoreCase = true)
            .replace("[protocol]", protocol, ignoreCase = true)
            .replace("[ua]", customUserAgent, ignoreCase = true)
            .replace("[cr]", "\r", ignoreCase = true)
            .replace("[lf]", "\n", ignoreCase = true)
            .replace("[crlf]", "\r\n", ignoreCase = true)

        // If template doesn't end with double CRLF, ensure it
        if (!processed.endsWith("\r\n\r\n") && !processed.contains("[split]")) {
            if (processed.endsWith("\r\n")) {
                processed += "\r\n"
            } else {
                processed += "\r\n\r\n"
            }
        }

        // Support [split] and [split_delay]
        return if (processed.contains("[split]", ignoreCase = true)) {
            processed.split(Regex("\\[split\\]", RegexOption.IGNORE_CASE))
        } else {
            listOf(processed)
        }
    }

    fun generatePayload(
        method: String,
        targetHost: String,
        targetPort: Int,
        proxyHost: String = "",
        injectMethod: String = "Normal",
        enableKeepAlive: Boolean = true,
        enableOnlineHost: Boolean = true,
        enableForwardHost: Boolean = false,
        enableReverseProxy: Boolean = false,
        enableUserAgent: Boolean = true,
        splitPayload: Boolean = false
    ): String {
        val sb = StringBuilder()
        val hostToUse = if (proxyHost.isNotBlank()) proxyHost else "[host]"

        when (injectMethod) {
            "Front Inject" -> {
                sb.append("GET http://$hostToUse/ [protocol][crlf]")
                if (enableOnlineHost) sb.append("Host: $hostToUse[crlf]")
                sb.append("[crlf]")
                sb.append("$method [host_port] [protocol][crlf]")
            }
            "Back Inject" -> {
                sb.append("$method [host_port] [protocol][crlf]")
                if (enableOnlineHost) sb.append("Host: [host][crlf]")
                sb.append("[crlf]")
                sb.append("GET http://$hostToUse/ [protocol][crlf]")
            }
            "Split" -> {
                sb.append("$method [host_port] [protocol][crlf]")
                sb.append("[split]")
                sb.append("Host: [host][crlf]")
            }
            else -> {
                // Normal
                sb.append("$method [host_port] [protocol][crlf]")
                sb.append("Host: [host][crlf]")
            }
        }

        if (enableOnlineHost && !sb.contains("X-Online-Host")) {
            sb.append("X-Online-Host: [host][crlf]")
        }
        if (enableForwardHost) {
            sb.append("X-Forward-Host: [host][crlf]")
        }
        if (enableReverseProxy) {
            sb.append("X-Forwarded-For: [host][crlf]")
        }
        if (enableKeepAlive) {
            sb.append("Connection: Keep-Alive[crlf]")
        }
        if (enableUserAgent) {
            sb.append("User-Agent: [ua][crlf]")
        }

        if (splitPayload && !sb.contains("[split]")) {
            sb.append("[split]")
        }

        sb.append("[crlf]")
        return sb.toString()
    }
}
