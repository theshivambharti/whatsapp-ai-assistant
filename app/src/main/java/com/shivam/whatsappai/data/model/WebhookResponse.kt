package com.shivam.whatsappai.data.model

import com.google.gson.annotations.SerializedName

data class WebhookResponse(
    @SerializedName("reply") val reply: String?
)
