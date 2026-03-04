package com.dirosky.asianbets

import android.app.Application
import com.dirosky.asianbets.utils.NotificationHelper

class AsianBetsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Criar canais de notificação
        NotificationHelper.createNotificationChannels(this)
    }
}
