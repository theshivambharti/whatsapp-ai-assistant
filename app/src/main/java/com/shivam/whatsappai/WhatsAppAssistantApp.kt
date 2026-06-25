package com.shivam.whatsappai

import android.app.Application
import com.shivam.whatsappai.data.DataStoreManager

class WhatsAppAssistantApp : Application() {
    
    lateinit var dataStoreManager: DataStoreManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        dataStoreManager = DataStoreManager(this)
    }

    companion object {
        lateinit var instance: WhatsAppAssistantApp
            private set
    }
}
