package com.shivam.whatsappai

import android.content.Context
import android.content.pm.PackageManager

object DiagnosticsManager {
    @Volatile var lastNotification: String = "None"
    @Volatile var lastWebhookRequest: String = "None"
    @Volatile var lastWebhookResponse: String = "None"
    @Volatile var lastReplySent: String = "None"
    @Volatile var lastError: String = "None"
    @Volatile var lastWhatsAppPackage: String = "None"

    fun getWhatsAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo("com.whatsapp", 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Not Installed"
        }
    }
}
