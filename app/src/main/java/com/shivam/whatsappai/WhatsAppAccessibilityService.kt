package com.shivam.whatsappai

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.shivam.whatsappai.data.db.LogDbHelper
import kotlinx.coroutines.*

class WhatsAppAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        var isConnectedState = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnectedState = true
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("INCOMING", "Accessibility Service Connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isConnectedState = false
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("INCOMING", "Accessibility Service Unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnectedState = false
        serviceScope.cancel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // We only care about events in com.whatsapp package
        if (event.packageName != "com.whatsapp") {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        // Search for conversation contact/group name
        val contactName = findContactName(rootNode)
        
        if (contactName != null) {
            val replyText = WhatsAppReplyManager.getReplyForContact(contactName)
            if (!replyText.isNullOrBlank()) {
                sendAutoReply(contactName, replyText)
            }
        } else {
            // Fallback: If contact name is not found, but we have a fallback pending reply, try to send it
            val fallback = WhatsAppReplyManager.fallbackReply
            if (!fallback.isNullOrBlank()) {
                sendAutoReply("Active Chat", fallback)
            }
        }
    }

    private fun findContactName(rootNode: AccessibilityNodeInfo): String? {
        // Method 1: ID search (WhatsApp classic ID for contact name in Toolbar/Action bar)
        val contactNameNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
        if (!contactNameNodes.isNullOrEmpty()) {
            val name = contactNameNodes[0].text?.toString()
            if (!name.isNullOrBlank()) {
                return name
            }
        }

        // Method 2: ID search for alternative IDs
        val chatNameNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/chat_name")
        if (!chatNameNodes.isNullOrEmpty()) {
            val name = chatNameNodes[0].text?.toString()
            if (!name.isNullOrBlank()) {
                return name
            }
        }

        // Method 3: Recursive fallback search for top-bar text views
        return findContactNameRecursively(rootNode)
    }

    private fun findContactNameRecursively(node: AccessibilityNodeInfo): String? {
        if (node.className == "android.widget.TextView" && node.isClickable) {
            // Usually the contact profile/header text in WhatsApp is clickable to see user details
            val text = node.text?.toString()
            if (!text.isNullOrBlank() && text.length > 2 && !text.contains(":") && !text.all { it.isDigit() }) {
                // Ignore timestamp or status bar info, return candidate name
                return text
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findContactNameRecursively(child)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun sendAutoReply(contactName: String, replyText: String) {
        if (WhatsAppReplyManager.isSending) return
        WhatsAppReplyManager.isSending = true

        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("REPLY_SENT", "Initiated reply sequence to '$contactName'. Waiting up to 5s for WhatsApp UI...")

        serviceScope.launch {
            var success = false
            val startTime = System.currentTimeMillis()
            val timeout = 5000L

            while (System.currentTimeMillis() - startTime < timeout) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val inputNode = findInputField(rootNode)
                    if (inputNode != null) {
                        // Type the message
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replyText)
                        val setSuccess = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                        if (setSuccess) {
                            logger?.addLog("REPLY_SENT", "Text typed successfully into input box.")
                            
                            // Let's find the send button and click it
                            val sendButton = findSendButton(rootNode)
                            if (sendButton != null) {
                                val clickSuccess = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                if (clickSuccess) {
                                    val msg = "Auto-reply sent successfully to '$contactName': '$replyText'"
                                    logger?.addLog("REPLY_SENT", msg)
                                    DiagnosticsManager.lastReplySent = "To: $contactName, Msg: $replyText"
                                    WhatsAppReplyManager.clearReplyForContact(contactName)
                                    success = true
                                    break
                                } else {
                                    logger?.addLog("ERROR", "Failed to click WhatsApp Send button. Retrying UI search...")
                                }
                            } else {
                                logger?.addLog("ERROR", "Could not locate WhatsApp Send button. Retrying UI search...")
                            }
                        } else {
                            logger?.addLog("ERROR", "Failed to type text using SET_TEXT action. Retrying...")
                        }
                    }
                }
                delay(300) // retry loop delay
            }

            if (!success) {
                val errMsg = "Failed to send auto-reply to '$contactName' after 5 seconds timeout"
                logger?.addLog("ERROR", errMsg)
                DiagnosticsManager.lastError = errMsg
            }
            WhatsAppReplyManager.isSending = false
        }
    }

    private fun findInputField(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Method 1: ID search (WhatsApp text entry field)
        val entryNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (!entryNodes.isNullOrEmpty()) {
            return entryNodes[0]
        }

        // Method 2: Recursive search with fallbacks
        return findInputFieldRecursively(rootNode)
    }

    private fun findInputFieldRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = node.className?.toString() ?: ""
        if (className == "android.widget.EditText" || node.isEditable) {
            return node
        }
        
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        if (desc.contains("type a message") || desc.contains("message") || desc.contains("escribe") || desc.contains("escrever") ||
            text.contains("type a message") || text.contains("message") || text.contains("escribe") || text.contains("escrever")) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findInputFieldRecursively(child)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun findSendButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Method 1: ID search
        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (!sendNodes.isNullOrEmpty()) {
            return sendNodes[0]
        }

        // Method 2: Recursive fallback search for ImageButton or Clickable View with specific descriptions
        return findSendButtonRecursively(rootNode)
    }

    private fun findSendButtonRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val className = node.className?.toString() ?: ""
        val isClickableType = className.contains("Button") || className.contains("Image") || node.isClickable
        
        if (isClickableType && (
            desc.contains("send") || desc.contains("dispatch") || desc.contains("enviar") || 
            desc.contains("envoyer") || desc.contains("mandar") || desc.contains("submit") || 
            desc.contains("deliver")
        )) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButtonRecursively(child)
            if (result != null) {
                return result
            }
        }
        return null
    }

    override fun onInterrupt() {
        isConnectedState = false
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("ERROR", "Accessibility Service interrupted")
    }
}
