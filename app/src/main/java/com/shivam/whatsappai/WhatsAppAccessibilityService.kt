package com.shivam.whatsappai

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.shivam.whatsappai.data.db.LogDbHelper

class WhatsAppAccessibilityService : AccessibilityService() {

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
                sendAutoReply(rootNode, contactName, replyText)
            }
        } else {
            // Fallback: If contact name is not found, but we have a fallback pending reply, try to send it
            val fallback = WhatsAppReplyManager.fallbackReply
            if (!fallback.isNullOrBlank()) {
                sendAutoReply(rootNode, "Active Chat", fallback)
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

    private fun sendAutoReply(rootNode: AccessibilityNodeInfo, contactName: String, replyText: String) {
        if (WhatsAppReplyManager.isSending) return
        WhatsAppReplyManager.isSending = true

        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper

        // Find input text field
        val inputNode = findInputField(rootNode)
        if (inputNode == null) {
            WhatsAppReplyManager.isSending = false
            return
        }

        // Enter text
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replyText)
        val setSuccess = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

        if (!setSuccess) {
            WhatsAppReplyManager.isSending = false
            logger?.addLog("ERROR", "Failed to type reply into input box")
            return
        }

        // Find send button
        val sendButton = findSendButton(rootNode)
        if (sendButton != null) {
            val clickSuccess = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clickSuccess) {
                logger?.addLog("REPLY_SENT", "Auto-reply sent successfully to '$contactName': '$replyText'")
                WhatsAppReplyManager.clearReplyForContact(contactName)
            } else {
                logger?.addLog("ERROR", "Failed to click WhatsApp Send button")
            }
        } else {
            logger?.addLog("ERROR", "Could not locate WhatsApp Send button")
        }

        WhatsAppReplyManager.isSending = false
    }

    private fun findInputField(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Method 1: ID search (WhatsApp text entry field)
        val entryNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (!entryNodes.isNullOrEmpty()) {
            return entryNodes[0]
        }

        // Method 2: Recursive search for EditText
        return findInputFieldRecursively(rootNode)
    }

    private fun findInputFieldRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText") {
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

        // Method 2: Recursive search for clickable ImageButton or View with description "Send"
        return findSendButtonRecursively(rootNode)
    }

    private fun findSendButtonRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (node.isClickable && (node.className == "android.widget.ImageButton" || node.className == "android.widget.ImageView") &&
            (desc.contains("send") || desc.contains("dispatch") || desc.contains("enviar"))) {
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
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("ERROR", "Accessibility Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val logger = (applicationContext as? WhatsAppAssistantApp)?.logDbHelper
        logger?.addLog("INCOMING", "Accessibility Service Connected")
    }
}
