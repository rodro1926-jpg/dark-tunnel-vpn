package com.example.model

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

data class TrafficStats(
    val bytesDown: Long = 0L,
    val bytesUp: Long = 0L,
    val speedDownBps: Long = 0L,
    val speedUpBps: Long = 0L,
    val pingMs: Long = -1L,
    val durationSeconds: Long = 0L,
    val publicIp: String = "Detecting..."
) {
    fun formattedDuration(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formattedDownBytes(): String = formatBytes(bytesDown)
    fun formattedUpBytes(): String = formatBytes(bytesUp)
    fun formattedDownSpeed(): String = formatBytes(speedDownBps) + "/s"
    fun formattedUpSpeed(): String = formatBytes(speedUpBps) + "/s"

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format("%.2f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
        }
    }
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, SUCCESS
}
