package com.hege.dingpay

import android.app.Application
import com.hege.dingpay.di.AppContainer
import com.hege.dingpay.service.ListenerSelfHealReceiver

class DingPayApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ListenerSelfHealReceiver.schedule(this)
    }
}
