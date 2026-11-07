package com.zcodego.app

import android.app.Application
import com.zcodego.app.data.AppController
import com.zcodego.app.data.LinkInbox
import com.zcodego.app.data.SessionRepository
import com.zcodego.app.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class ZCodeGoApp : Application() {
    lateinit var controller: AppController
        private set

    val processInForeground = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        val settingsRepository = SettingsRepository(this)
        val sessionRepository = SessionRepository(this)
        controller = AppController(settingsRepository, sessionRepository, LinkInbox())
    }
}
