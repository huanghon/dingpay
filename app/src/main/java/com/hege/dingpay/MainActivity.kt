package com.hege.dingpay

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hege.dingpay.service.ListenerSelfHealReceiver
import com.hege.dingpay.service.PaymentNotificationListenerService
import com.hege.dingpay.service.SystemStatus
import com.hege.dingpay.ui.AppRoot
import com.hege.dingpay.ui.theme.DingPayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DingPayTheme(dynamicColor = false) {
                AppRoot(container = (application as DingPayApplication).container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SystemStatus.requestNotificationListenerRebind(this)
        val synced = PaymentNotificationListenerService.requestActiveNotificationSync()
        ListenerSelfHealReceiver.heal(this, "activity_resume")
        ListenerSelfHealReceiver.schedule(this)
        Log.w("DingPayMain", "onResume requested listener rebind; activeSync=$synced")
    }
}
