package com.shivam.whatsappai.data.model

import com.google.gson.annotations.SerializedName

data class TestRequest(
    @SerializedName("app") val app: String = "WhatsApp",
    @SerializedName("sender") val sender: String = "Test User",
    @SerializedName("phone") val phone: String = "9999999999",
    @SerializedName("message") val message: String = "Hello"
)
