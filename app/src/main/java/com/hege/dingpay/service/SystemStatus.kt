package com.hege.dingpay.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService

object SystemStatus {
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val component = ComponentName(context, PaymentNotificationListenerService::class.java)
        val flat = component.flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabled.split(":").any { it.equals(flat, ignoreCase = true) }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestNotificationListenerRebind(context: Context) {
        if (!isNotificationListenerEnabled(context)) return
        val component = ComponentName(context, PaymentNotificationListenerService::class.java)
        NotificationListenerService.requestRebind(component)
    }

    /**
     * 强制重连通知监听：先把监听组件禁用再启用（DONT_KILL_APP），
     * 触发系统重新绑定，随后再显式 requestRebind。
     * 这是解决"权限已授予但监听回调假死"的标准手法。
     */
    fun forceReconnectNotificationListener(context: Context) {
        val component = ComponentName(context, PaymentNotificationListenerService::class.java)
        val packageManager = context.packageManager
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        NotificationListenerService.requestRebind(component)
    }

    /** 闹钟流音量大于 0 才能听到播报（播报走 USAGE_ALARM / STREAM_ALARM）。 */
    fun isAlarmVolumeAudible(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(AudioManager.STREAM_ALARM) > 0
    }
}
