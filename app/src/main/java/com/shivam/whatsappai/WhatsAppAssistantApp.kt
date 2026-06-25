package com.shivam.whatsappai

import android.app.Application
import com.shivam.whatsappai.data.DataStoreManager
import com.shivam.whatsappai.data.db.LogDbHelper

class WhatsAppAssistantApp : Application() {
    
    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var logDbHelper: LogDbHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        dataStoreManager = DataStoreManager(this)
        logDbHelper = LogDbHelper(this)
    }

    companion object {
        lateinit var instance: WhatsAppAssistantApp
            private set
    }
}
