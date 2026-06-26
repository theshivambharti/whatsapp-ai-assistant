package com.shivam.whatsappai

import android.content.Context
import android.content.pm.PackageManager
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TestStage(
    val number: Int,
    val name: String,
    var status: String = "WAITING", // "WAITING", "SUCCESS", "FAILURE"
    var timestamp: String = "",
    var durationMs: Long = -1L,
    var details: String = "Waiting for pipeline trigger...",
    var exception: String? = null
)

object DiagnosticsManager {
    @Volatile var lastNotification: String = "None"
    @Volatile var lastWebhookRequest: String = "None"
    @Volatile var lastWebhookResponse: String = "None"
    @Volatile var lastReplySent: String = "None"
    @Volatile var lastError: String = "None"
    @Volatile var lastWhatsAppPackage: String = "None"

    val stages = listOf(
        TestStage(1, "Notification Received"),
        TestStage(2, "Sender Parsed"),
        TestStage(3, "Webhook Request Sent"),
        TestStage(4, "Webhook Response Received"),
        TestStage(5, "JSON Parsed"),
        TestStage(6, "Reply Stored"),
        TestStage(7, "WhatsApp Chat Detected"),
        TestStage(8, "Input Field Found"),
        TestStage(9, "Reply Typed"),
        TestStage(10, "Send Button Found"),
        TestStage(11, "Reply Sent")
    )

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun resetStages() {
        stages.forEach { stage ->
            stage.status = "WAITING"
            stage.timestamp = ""
            stage.durationMs = -1L
            stage.details = "Waiting for pipeline trigger..."
            stage.exception = null
        }
    }

    @Synchronized
    fun updateStageSuccess(number: Int, details: String, durationMs: Long = -1L) {
        stages.find { it.number == number }?.apply {
            this.status = "SUCCESS"
            this.timestamp = sdf.format(Date())
            this.durationMs = durationMs
            this.details = details
            this.exception = null
        }
    }

    @Synchronized
    fun updateStageFailure(number: Int, details: String, exception: Throwable? = null, durationMs: Long = -1L) {
        stages.find { it.number == number }?.apply {
            this.status = "FAILURE"
            this.timestamp = sdf.format(Date())
            this.durationMs = durationMs
            this.details = details
            this.exception = exception?.let {
                val sw = StringWriter()
                it.printStackTrace(PrintWriter(sw))
                sw.toString()
            }
        }
        // Mark subsequent stages as waiting/stopped
        for (i in (number + 1)..11) {
            stages.find { i == it.number }?.apply {
                this.status = "WAITING"
                this.timestamp = ""
                this.durationMs = -1L
                this.details = "Pipeline stopped due to failure at Stage $number."
                this.exception = null
            }
        }
    }

    fun getWhatsAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo("com.whatsapp", 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Not Installed"
        }
    }
}
