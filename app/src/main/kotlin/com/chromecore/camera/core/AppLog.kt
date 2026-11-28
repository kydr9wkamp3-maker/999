package com.chromecore.camera.core

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ring-buffer in-memory log with Android Logcat output.
 * Tags by module for filtering.
 */
object AppLog {

    private const val TAG = "CCX"
    private const val MAX_ENTRIES = 200

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String
    )

    private val entries = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun v(tag: String, message: String) {
        Log.v("$TAG:$tag", message)
        addEntry("V", tag, message)
    }

    fun d(tag: String, message: String) {
        Log.d("$TAG:$tag", message)
        addEntry("D", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
        addEntry("I", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w("$TAG:$tag", message)
        addEntry("W", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG:$tag", message, throwable)
        } else {
            Log.e("$TAG:$tag", message)
        }
        addEntry("E", tag, if (throwable != null) "$message: ${throwable.message}" else message)
    }

    private fun addEntry(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        entries.addLast(LogEntry(timestamp, level, tag, message))
        while (entries.size > MAX_ENTRIES) {
            entries.removeFirst()
        }
    }

    fun getEntries(): List<LogEntry> = entries.toList()

    fun getEntries(tag: String): List<LogEntry> = entries.filter { it.tag == tag }

    fun clear() {
        entries.clear()
    }

    fun export(): String = buildString {
        appendLine("CHROMECORE X — App Log")
        appendLine("=".repeat(50))
        entries.forEach { entry ->
            appendLine("[${entry.timestamp}] ${entry.level}/${entry.tag}: ${entry.message}")
        }
    }
}
