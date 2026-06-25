package com.shivam.whatsappai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.shivam.whatsappai.data.model.WebhookRequest
import com.shivam.whatsappai.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isConnected = false

    companion object {
        private const val CHANNEL_ID = "WhatsAppAssistantChannel"
        private const val NOTIFICATION_ID = 1001
        var isRunning = false
            private set
        var isConnectedState = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startServiceInForeground()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        isConnectedState = true
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("INCOMING", "Notification Listener Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        isConnectedState = false
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("INCOMING", "Notification Listener Disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isConnectedState = false
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("INCOMING", "Notification Listener Destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if (sbn.packageName != "com.whatsapp") {
            return
        }

        serviceScope.launch {
            try {
                processWhatsAppNotification(sbn)
            } catch (e: Exception) {
                val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
                logger?.addLog("ERROR", "Error processing notification: ${e.message}")
            }
        }
    }

    private suspend fun processWhatsAppNotification(sbn: StatusBarNotification) {
        val app = applicationContext as WhatsAppAssistantApp
        val logger = app.logDbHelper

        // Check if the service is toggled ON by the user
        val isServiceActive = app.dataStoreManager.serviceActiveFlow.first()
        if (!isServiceActive) {
            return
        }

        val notification = sbn.notification
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) {
            return
        }

        // Use MessagingStyle parser to extract correct sender name and actual content
        val messagesStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        var senderName = title
        var messageText = text
        var isGroup = false

        if (messagesStyle != null) {
            isGroup = messagesStyle.isGroupConversation
            val lastMessage = messagesStyle.messages.lastOrNull()
            if (lastMessage != null) {
                senderName = lastMessage.person?.name?.toString() ?: title
                messageText = lastMessage.text?.toString() ?: text
            }
        } else {
            isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
                      extras.containsKey(Notification.EXTRA_CONVERSATION_TITLE)
        }

        // Standardize ignore checks to filter out calls, typing, status, media, deleted messages
        val lowerText = messageText.lowercase()
        val lowerTitle = senderName.lowercase()

        val isSystemNotification = sbn.id == 1 || lowerTitle.contains("whatsapp") || lowerText.contains("whatsapp web")
        val isCall = lowerText.contains("missed voice call") ||
                     lowerText.contains("missed video call") ||
                     lowerText.contains("voice call") ||
                     lowerText.contains("video call") ||
                     lowerText.contains("calling...") ||
                     lowerText.contains("incoming voice call") ||
                     lowerText.contains("incoming video call")
        val isTyping = lowerText.contains("typing...")
        val isDeleted = lowerText.contains("this message was deleted")
        val isBackupOrSystem = lowerText.contains("checking for new messages") ||
                               lowerText.contains("finished downloading") ||
                               lowerText.contains("backup")
        val isMediaPlaceholder = messageText == "📷 Photo" ||
                                 messageText == "🎥 Video" ||
                                 messageText == "🎵 Audio" ||
                                 messageText == "GIF" ||
                                 messageText == "📄 Document" ||
                                 messageText == "👾 Sticker"

        if (isSystemNotification || isCall || isTyping || isDeleted || isBackupOrSystem || isMediaPlaceholder || messageText.isBlank()) {
            return
        }

        // Determine phone number if available from sender title
        var phone = "Unknown"
        val digitOnly = senderName.filter { it.isDigit() }
        if (digitOnly.length >= 7) {
            phone = digitOnly
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date(sbn.postTime))

        // Record diagnostics
        DiagnosticsManager.lastNotification = "From: $senderName, Msg: $messageText, Time: $timestamp"
        DiagnosticsManager.lastWhatsAppPackage = sbn.packageName

        logger.addLog("INCOMING", "Incoming message from '$senderName' ($phone): '$messageText'")

        // Retrieve config
        val serverUrl = app.dataStoreManager.serverUrlFlow.first()
        if (serverUrl.isBlank()) {
            val errMsg = "Webhook dispatch aborted: Server URL is empty."
            logger.addLog("ERROR", errMsg)
            DiagnosticsManager.lastError = errMsg
            return
        }

        val hName = app.dataStoreManager.headerNameFlow.first()
        val hValue = app.dataStoreManager.headerValueFlow.first()
        val headers = mutableMapOf<String, String>()
        if (hName.isNotBlank() && hValue.isNotBlank()) {
            headers[hName] = hValue
        }

        val webhookRequest = WebhookRequest(
            app = "WhatsApp",
            sender = senderName,
            phone = phone,
            message = messageText,
            timestamp = timestamp,
            isGroup = isGroup
        )

        // Webhook invocation with automatic exponential backoff retry (up to 3 times)
        serviceScope.launch {
            try {
                val reqMsg = "URL: $serverUrl, Body: $senderName says $messageText"
                DiagnosticsManager.lastWebhookRequest = reqMsg
                
                val response = retryWithBackoff(times = 3, initialDelay = 1500) {
                    logger.addLog("WEBHOOK_REQ", "POST to $serverUrl")
                    RetrofitClient.apiService.sendWebhook(serverUrl, headers, webhookRequest)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val replyText = body?.reply
                    DiagnosticsManager.lastWebhookResponse = "Code: ${response.code()}, Reply: $replyText"
                    if (!replyText.isNullOrBlank()) {
                        // Pass reply to AccessibilityService layer and open conversation
                        WhatsAppReplyManager.addPendingReply(
                            context = applicationContext,
                            sender = senderName,
                            reply = replyText,
                            contentIntent = notification.contentIntent
                        )
                    } else {
                        logger.addLog("WEBHOOK_RES", "Webhook responded with blank reply")
                    }
                } else {
                    val errBody = response.errorBody()?.string() ?: "Unknown error"
                    val errMsg = "Webhook failed with code ${response.code()}: $errBody"
                    logger.addLog("ERROR", errMsg)
                    DiagnosticsManager.lastError = errMsg
                    DiagnosticsManager.lastWebhookResponse = "Code: ${response.code()}, Error: $errBody"
                }
            } catch (e: Exception) {
                val errMsg = "Webhook dispatch failed after retries: ${e.message}"
                logger.addLog("ERROR", errMsg)
                DiagnosticsManager.lastError = errMsg
                DiagnosticsManager.lastWebhookResponse = "Failed: ${e.message}"
            }
        }
    }

    private suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                logger?.addLog("ERROR", "Attempt ${attempt + 1} failed: ${e.message}. Retrying...")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
        return block()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "WhatsApp AI Assistant Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = "Keeps the WhatsApp Auto-Responder running in background"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startServiceInForeground() {
        val notificationIntent = Intent(this, com.shivam.whatsappai.ui.home.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WhatsApp AI Assistant Active")
            .setContentText("Listening for incoming WhatsApp messages...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
