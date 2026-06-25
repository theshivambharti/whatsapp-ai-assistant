package com.shivam.whatsappai.data.repository

import com.shivam.whatsappai.data.DataStoreManager
import com.shivam.whatsappai.data.model.TestRequest
import com.shivam.whatsappai.data.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepository(private val dataStoreManager: DataStoreManager) {

    val serverUrl: Flow<String> = dataStoreManager.serverUrlFlow
    val headerName: Flow<String> = dataStoreManager.headerNameFlow
    val headerValue: Flow<String> = dataStoreManager.headerValueFlow
    val serviceActive: Flow<Boolean> = dataStoreManager.serviceActiveFlow

    suspend fun saveSettings(serverUrl: String, headerName: String, headerValue: String) {
        dataStoreManager.saveSettings(serverUrl, headerName, headerValue)
    }

    suspend fun setServiceActive(active: Boolean) {
        dataStoreManager.setServiceActive(active)
    }

    suspend fun sendTestRequest(): Result<TestResponseData> {
        return try {
            val url = serverUrl.first()
            if (url.isBlank()) {
                return Result.failure(Exception("Server URL is empty. Please configure it first."))
            }

            val hName = headerName.first()
            val hValue = headerValue.first()

            val headers = mutableMapOf<String, String>()
            if (hName.isNotBlank() && hValue.isNotBlank()) {
                headers[hName] = hValue
            }

            val request = TestRequest()
            val response = RetrofitClient.apiService.sendTestRequest(url, headers, request)

            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: "No content in response"
                Result.success(TestResponseData(response.code(), bodyString))
            } else {
                val errorString = response.errorBody()?.string() ?: "Unknown error occurred"
                Result.success(TestResponseData(response.code(), errorString, isSuccess = false))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class TestResponseData(
    val code: Int,
    val body: String,
    val isSuccess: Boolean = true
)
