package com.hege.dingpay.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

class ListenerSelfHealReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val reason = intent?.action.orEmpty().ifBlank { "unknown" }
        heal(appContext, reason)
        schedule(appContext)
    }

    companion object {
        private const val TAG = "DingPayListenerHeal"
        private const val ACTION_SELF_HEAL = "com.hege.dingpay.action.LISTENER_SELF_HEAL"
        private const val REQUEST_CODE = 5201
        private const val HEAL_INTERVAL_MS = 10 * 60 * 1000L

        fun schedule(context: Context) {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(AlarmManager::class.java)
            val triggerAt = SystemClock.elapsedRealtime() + HEAL_INTERVAL_MS
            val pendingIntent = pendingIntent(appContext)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
                Log.w(TAG, "Scheduled listener self-heal alarm")
            }.onFailure { error ->
                Log.w(TAG, "Failed to schedule listener self-heal alarm", error)
            }
        }

        fun heal(context: Context, reason: String) {
            if (!SystemStatus.isNotificationListenerEnabled(context)) {
                Log.w(TAG, "Skip listener self-heal because permission is disabled reason=$reason")
                return
            }
            SystemStatus.requestNotificationListenerRebind(context)
            val activeSynced = PaymentNotificationListenerService.requestActiveNotificationSync()
            if (!activeSynced) {
                runCatching { SystemStatus.forceReconnectNotificationListener(context) }
                    .onFailure { error -> Log.w(TAG, "Force listener reconnect failed", error) }
            }
            Log.w(TAG, "Listener self-heal done reason=$reason activeSynced=$activeSynced")
        }

        private fun pendingIntent(context: Context): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, ListenerSelfHealReceiver::class.java).setAction(ACTION_SELF_HEAL),
                flags
            )
        }
    }
}
