package com.shivam.whatsappai

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.shivam.whatsappai.data.db.LogDbHelper
import java.util.concurrent.ConcurrentHashMap

object WhatsAppReplyManager {

    // Maps contact/group name (lowercase) to the reply message
    private val replyQueue = ConcurrentHashMap<String, String>()
    
    // Fallback message if contact/group name cannot be parsed from accessibility window
    @Volatile
    var fallbackReply: String? = null
        private set

    @Volatile
    var isSending = false

    fun addPendingReply(context: Context, sender: String, reply: String, contentIntent: PendingIntent?) {
        val logger = (context.applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        
        val key = sender.trim().lowercase()
        replyQueue[key] = reply
        fallbackReply = reply
        
        logger?.addLog("WEBHOOK_RES", "Response received for '$sender': '$reply'")

        // Trigger opening of WhatsApp conversation window
        if (contentIntent != null) {
            try {
                logger?.addLog("REPLY_SENT", "Opening WhatsApp conversation for '$sender'...")
                contentIntent.send()
            } catch (e: Exception) {
                logger?.addLog("ERROR", "Failed to send contentIntent: ${e.message}. Launching WhatsApp directly.")
                launchWhatsAppDirectly(context)
            }
        } else {
            logger?.addLog("REPLY_SENT", "No direct intent available. Launching WhatsApp directly.")
            launchWhatsAppDirectly(context)
        }
    }

    private fun launchWhatsAppDirectly(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            val logger = (context.applicationContext as? WhatsAppAssistantApp)?.logDbHelper
            logger?.addLog("ERROR", "Could not launch WhatsApp: ${e.message}")
        }
    }

    fun getReplyForContact(contactName: String): String? {
        val cleanName = contactName.trim().lowercase()
        // Try exact match
        var reply = replyQueue[cleanName]
        if (reply != null) {
            return reply
        }

        // Try partial match (e.g., if conversation name is "John (Work)" and sender is "John")
        for ((key, value) in replyQueue) {
            if (cleanName.contains(key) || key.contains(cleanName)) {
                return value
            }
        }

        // Fallback to latest reply
        return fallbackReply
    }

    fun clearReplyForContact(contactName: String) {
        val cleanName = contactName.trim().lowercase()
        replyQueue.remove(cleanName)
        // Also remove partials
        val keysToRemove = mutableListOf<String>()
        for ((key, _) in replyQueue) {
            if (cleanName.contains(key) || key.contains(cleanName)) {
                keysToRemove.add(key)
            }
        }
        for (key in keysToRemove) {
            replyQueue.remove(key)
        }

        fallbackReply = null
        isSending = false
    }

    fun clearAll() {
        replyQueue.clear()
        fallbackReply = null
        isSending = false
    }
}
