package com.shivam.whatsappai.data.network

import com.shivam.whatsappai.data.model.TestRequest
import com.shivam.whatsappai.data.model.WebhookRequest
import com.shivam.whatsappai.data.model.WebhookResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {

    @POST
    suspend fun sendTestRequest(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: TestRequest
    ): Response<ResponseBody>

    @POST
    suspend fun sendWebhook(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: WebhookRequest
    ): Response<WebhookResponse>
}
