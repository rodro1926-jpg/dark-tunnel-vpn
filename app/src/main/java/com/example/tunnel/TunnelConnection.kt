package com.example.tunnel

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class TunnelConnection(
    val socket: Socket,
    val inputStream: InputStream,
    val outputStream: OutputStream,
    val protocolName: String
) {
    private val closed = AtomicBoolean(false)

    fun isConnected(): Boolean {
        return !closed.get() && socket.isConnected && !socket.isClosed
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                inputStream.close()
            } catch (_: Exception) {}
            try {
                outputStream.close()
            } catch (_: Exception) {}
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }
}
