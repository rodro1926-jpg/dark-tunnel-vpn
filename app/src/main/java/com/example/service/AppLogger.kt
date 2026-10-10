package com.example.service

import com.example.model.LogEntry
import com.example.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.LinkedList

object AppLogger {
    private const val MAX_LOGS = 500
    private val buffer = Collections.synchronizedList(LinkedList<LogEntry>())
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    fun log(level: LogLevel, message: String) {
        val entry = LogEntry(level = level, message = message)
        buffer.add(entry)
        while (buffer.size > MAX_LOGS) {
            buffer.removeAt(0)
        }
        _logsFlow.value = ArrayList(buffer)
    }

    fun d(message: String) = log(LogLevel.DEBUG, message)
    fun i(message: String) = log(LogLevel.INFO, message)
    fun w(message: String) = log(LogLevel.WARN, message)
    fun e(message: String) = log(LogLevel.ERROR, message)
    fun s(message: String) = log(LogLevel.SUCCESS, message)

    fun clear() {
        buffer.clear()
        _logsFlow.value = emptyList()
    }
}
