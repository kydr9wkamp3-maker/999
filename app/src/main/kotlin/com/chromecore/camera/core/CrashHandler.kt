package com.chromecore.camera.core

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler — writes crash reports to Documents/ChromecoreX/diagnostics/
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = buildCrashReport(thread, throwable)
            saveCrashReport(report)
            AppLog.e("CrashHandler", "Crash saved: ${report.first}")
        } catch (e: Exception) {
            AppLog.e("CrashHandler", "Failed to save crash report", e)
        }

        // Let the default handler deal with the crash (shows system crash dialog)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun buildCrashReport(thread: Thread, throwable: Throwable): Pair<String, String> {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val filename = "crash-$timestamp.txt"

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val report = buildString {
            appendLine("CHROMECORE X — Crash Report")
            appendLine("=".repeat(50))
            appendLine("Date: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine()
            appendLine("Device:")
            appendLine("  Model: ${Build.MODEL}")
            appendLine("  Manufacturer: ${Build.MANUFACTURER}")
            appendLine("  Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("  Build: ${Build.DISPLAY}")
            appendLine()
            appendLine("App:")
            appendLine("  Version: 1.0.0")
            appendLine("  Build: 1")
            appendLine()
            appendLine("Exception:")
            appendLine("  Type: ${throwable.javaClass.name}")
            appendLine("  Message: ${throwable.message}")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(stackTrace)
        }

        return filename to report
    }

    private fun saveCrashReport(crash: Pair<String, String>) {
        val ctx = context ?: return
        val dir = File(ctx.getExternalFilesDir(null), "diagnostics")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, crash.first)
        file.writeText(crash.second)
    }
}
