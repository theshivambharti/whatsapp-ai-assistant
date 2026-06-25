package com.shivam.whatsappai.data.model

import com.google.gson.annotations.SerializedName

data class WebhookRequest(
    @SerializedName("app") val app: String = "WhatsApp",
    @SerializedName("sender") val sender: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("isGroup") val isGroup: Boolean
)
