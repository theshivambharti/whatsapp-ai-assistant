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
        logger?.addLog("REPLY_SENT", "Chat opened for contact: '$contactName'")
        logger?.addLog("REPLY_SENT", "Initiated reply sequence to '$contactName'. Waiting up to 5s for WhatsApp UI...")

        val autoReplyStartTime = System.currentTimeMillis()
        DiagnosticsManager.updateStageSuccess(7, "WhatsApp chat detected for contact '$contactName'", System.currentTimeMillis() - autoReplyStartTime)

        serviceScope.launch {
            var success = false
            val startTime = System.currentTimeMillis()
            val timeout = 5000L
            var lastErrorMsg = "Timeout finding input field or typing text"
            val lastException: Throwable? = null

            while (System.currentTimeMillis() - startTime < timeout) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val inputNode = findInputField(rootNode)
                    if (inputNode != null) {
                        val inputTime = System.currentTimeMillis() - startTime
                        DiagnosticsManager.updateStageSuccess(8, "Input field located in view tree hierarchy", inputTime)
                        logger?.addLog("REPLY_SENT", "Input field found successfully")
                        
                        // Type the message
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replyText)
                        val setSuccess = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                        if (setSuccess) {
                            val typeTime = System.currentTimeMillis() - startTime
                            DiagnosticsManager.updateStageSuccess(9, "Set text reply action completed successfully", typeTime)
                            logger?.addLog("REPLY_SENT", "Text typed into message box: '$replyText'")
                            
                            // Let's find the send button and click it
                            val sendButton = findSendButton(rootNode)
                            if (sendButton != null) {
                                val sendButtonTime = System.currentTimeMillis() - startTime
                                DiagnosticsManager.updateStageSuccess(10, "WhatsApp Send button successfully located", sendButtonTime)
                                logger?.addLog("REPLY_SENT", "Send button found successfully")
                                
                                val clickSuccess = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                if (clickSuccess) {
                                    val duration = System.currentTimeMillis() - startTime
                                    val msg = "Reply successfully sent to '$contactName'! (Execution time: ${duration}ms)"
                                    logger?.addLog("REPLY_SENT", msg)
                                    DiagnosticsManager.lastReplySent = "To: $contactName, Msg: $replyText"
                                    
                                    DiagnosticsManager.updateStageSuccess(11, "Send click performed successfully. Auto-reply completed", duration)
                                    
                                    WhatsAppReplyManager.clearReplyForContact(contactName)
                                    success = true
                                    break
                                } else {
                                    lastErrorMsg = "Failed to click WhatsApp Send button using performAction(ACTION_CLICK)."
                                    logger?.addLog("ERROR", "Failed to click WhatsApp Send button. Retrying UI search...")
                                }
                            } else {
                                lastErrorMsg = "Could not locate WhatsApp Send button in current root node."
                                logger?.addLog("ERROR", "Could not locate WhatsApp Send button. Retrying UI search...")
                            }
                        } else {
                            lastErrorMsg = "Failed to set text on input field using SET_TEXT action."
                            logger?.addLog("ERROR", "Failed to type text using SET_TEXT action. Retrying...")
                        }
                    } else {
                        lastErrorMsg = "WhatsApp message input field not found in rootNode hierarchy."
                    }
                } else {
                    lastErrorMsg = "rootInActiveWindow is null"
                }
                delay(300) // retry loop delay
            }

            if (!success) {
                val duration = System.currentTimeMillis() - startTime
                val errMsg = "Failed to send auto-reply to '$contactName' after 5 seconds timeout: $lastErrorMsg"
                logger?.addLog("ERROR", errMsg)
                DiagnosticsManager.lastError = errMsg
                val currentStage = getCurrentUnfinishedStage()
                DiagnosticsManager.updateStageFailure(currentStage, lastErrorMsg, lastException, duration)
            }
            WhatsAppReplyManager.isSending = false
        }
    }

    private fun getCurrentUnfinishedStage(): Int {
        for (i in 8..11) {
            val stage = DiagnosticsManager.stages.find { it.number == i }
            if (stage != null && stage.status == "WAITING") {
                return i
            }
        }
        return 11
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
