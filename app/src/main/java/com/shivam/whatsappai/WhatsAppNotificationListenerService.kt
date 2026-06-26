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

        val notiStartTime = System.currentTimeMillis()
        DiagnosticsManager.resetStages()
        DiagnosticsManager.updateStageSuccess(1, "Notification from WhatsApp captured: ID ${sbn.id}", System.currentTimeMillis() - notiStartTime)

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
            val parseDuration = System.currentTimeMillis() - notiStartTime
            DiagnosticsManager.updateStageFailure(2, "Notification ignored. SystemNotification=$isSystemNotification, Call=$isCall, Typing=$isTyping, Deleted=$isDeleted, BackupOrSystem=$isBackupOrSystem, MediaPlaceholder=$isMediaPlaceholder, Blank=${messageText.isBlank()}", durationMs = parseDuration)
            return
        }

        val parseDuration = System.currentTimeMillis() - notiStartTime
        DiagnosticsManager.updateStageSuccess(2, "Sender Parsed: '$senderName', Message: '$messageText', Group: $isGroup", parseDuration)

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

        logger.addLog("INCOMING", "Notification Received\nSender: $senderName\nPhone: $phone\nMessage: $messageText\nTimestamp: $timestamp")

        // Retrieve config
        val serverUrl = app.dataStoreManager.serverUrlFlow.first()
        if (serverUrl.isBlank()) {
            val errMsg = "Webhook dispatch aborted: Server URL is empty."
            logger.addLog("ERROR", errMsg)
            DiagnosticsManager.lastError = errMsg
            DiagnosticsManager.updateStageFailure(3, "Server URL is empty inside data store settings")
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
            val startTime = System.currentTimeMillis()
            try {
                val reqMsg = "URL: $serverUrl, Body: $senderName says $messageText"
                DiagnosticsManager.lastWebhookRequest = reqMsg
                
                DiagnosticsManager.updateStageSuccess(3, "Outgoing webhook payload prepared: $reqMsg", System.currentTimeMillis() - startTime)

                val response = retryWithBackoff(times = 3, initialDelay = 1500) {
                    logger.addLog("WEBHOOK_REQ", "Webhook called: POST to '$serverUrl'")
                    RetrofitClient.apiService.sendWebhook(serverUrl, headers, webhookRequest)
                }

                val duration = System.currentTimeMillis() - startTime
                logger.addLog("WEBHOOK_RES", "HTTP response received from '$serverUrl'. Code: ${response.code()} (Execution time: ${duration}ms)")
                DiagnosticsManager.updateStageSuccess(4, "HTTP response received. Status: ${response.code()}", duration)

                if (response.isSuccessful) {
                    val body = response.body()
                    val replyText = body?.reply
                    DiagnosticsManager.lastWebhookResponse = "Code: ${response.code()}, Reply: $replyText"
                    if (!replyText.isNullOrBlank()) {
                        logger.addLog("WEBHOOK_RES", "Reply successfully parsed: '$replyText' for contact '$senderName'")
                        DiagnosticsManager.updateStageSuccess(5, "JSON Parsed successfully. Reply: '$replyText'", System.currentTimeMillis() - startTime)
                        
                        // Pass reply to AccessibilityService layer and open conversation
                        WhatsAppReplyManager.addPendingReply(
                            context = applicationContext,
                            sender = senderName,
                            reply = replyText,
                            contentIntent = notification.contentIntent
                        )
                    } else {
                        logger.addLog("ERROR", "Reply parsed but it is empty/blank")
                        DiagnosticsManager.updateStageFailure(5, "JSON Parsed but reply was null or empty", durationMs = System.currentTimeMillis() - startTime)
                    }
                } else {
                    val errBody = response.errorBody()?.string() ?: "Unknown error"
                    val errMsg = "Webhook failed with code ${response.code()}: $errBody (Execution time: ${duration}ms)"
                    logger.addLog("ERROR", errMsg)
                    DiagnosticsManager.lastError = errMsg
                    DiagnosticsManager.lastWebhookResponse = "Code: ${response.code()}, Error: $errBody"
                    DiagnosticsManager.updateStageFailure(4, "Webhook responded with failure status: ${response.code()} ($errBody)", durationMs = duration)
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errMsg = "Webhook dispatch failed after retries: ${e.message} (Execution time: ${duration}ms)"
                logger.addLog("ERROR", errMsg)
                DiagnosticsManager.lastError = errMsg
                DiagnosticsManager.lastWebhookResponse = "Failed: ${e.message}"
                DiagnosticsManager.updateStageFailure(3, "Webhook dispatch failed after retries: ${e.message}", e, duration)
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
}
