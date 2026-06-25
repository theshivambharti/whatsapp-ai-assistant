package com.shivam.whatsappai.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.shivam.whatsappai.WhatsAppAssistantApp
import com.shivam.whatsappai.data.repository.SettingsRepository
import com.shivam.whatsappai.data.repository.TestResponseData
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository
) : AndroidViewModel(application) {

    val serverUrl: LiveData<String> = repository.serverUrl.asLiveData()
    val headerName: LiveData<String> = repository.headerName.asLiveData()
    val headerValue: LiveData<String> = repository.headerValue.asLiveData()
    val serviceActive: LiveData<Boolean> = repository.serviceActive.asLiveData()

    private val _testRequestState = MutableLiveData<TestRequestState>()
    val testRequestState: LiveData<TestRequestState> = _testRequestState

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    fun saveSettings(url: String, headerName: String, headerValue: String) {
        viewModelScope.launch {
            repository.saveSettings(url, headerName, headerValue)
            _saveStatus.postValue(true)
        }
    }

    fun toggleService() {
        viewModelScope.launch {
            val current = serviceActive.value ?: true
            repository.setServiceActive(!current)
        }
    }

    fun sendTestRequest() {
        _testRequestState.value = TestRequestState.Loading
        viewModelScope.launch {
            val result = repository.sendTestRequest()
            result.onSuccess { data ->
                _testRequestState.postValue(TestRequestState.Success(data))
            }.onFailure { exception ->
                _testRequestState.postValue(TestRequestState.Error(exception.message ?: "Unknown Error"))
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = false
    }

    fun resetTestState() {
        _testRequestState.value = TestRequestState.Idle
    }
}

sealed class TestRequestState {
    object Idle : TestRequestState()
    object Loading : TestRequestState()
    data class Success(val response: TestResponseData) : TestRequestState()
    data class Error(val message: String) : TestRequestState()
}

class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val app = application as WhatsAppAssistantApp
            val repository = SettingsRepository(app.dataStoreManager)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
