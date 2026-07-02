package com.hege.dingpay.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.hege.dingpay.DingPayApplication
import com.hege.dingpay.data.NotificationDiagnostic
import com.hege.dingpay.data.NotificationPayload
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        activeInstance = WeakReference(this)
        Log.w(TAG, "Notification listener service created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.w(TAG, "onNotificationPosted package=${sbn.packageName} key=${sbn.key}")
        processNotification(sbn)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeInstance = WeakReference(this)
        Log.w(TAG, "Notification listener connected")
        syncActiveNotifications("connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected; requesting rebind")
        requestRebind(ComponentName(this, PaymentNotificationListenerService::class.java))
    }

    private fun syncActiveNotifications(reason: String) {
        val notifications = activeNotifications.orEmpty()
        Log.w(TAG, "Sync active notifications reason=$reason count=${notifications.size}")
        notifications.forEach { sbn ->
            processNotification(sbn)
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val payload = NotificationTextExtractor.extract(sbn) ?: return
        Log.w(TAG, "Received notification package=${payload.packageName} title=${payload.title}")
        val container = (application as DingPayApplication).container
        serviceScope.launch {
            runCatching {
                if (!container.licenseRepository.isBroadcastAllowed()) {
                    Log.w(TAG, "Skip notification because license is inactive")
                    saveDiagnostic(
                        payload,
                        NotificationDiagnostic.STAGE_LICENSE_INACTIVE,
                        "license inactive or expired"
                    )
                    return@launch
                }
                val rules = container.settingsStore.rulesFlow.first()
                val rule = PaymentRuleMatcher.match(payload, rules)
                if (rule == null) {
                    Log.d(TAG, "Skip notification because no rule matched package=${payload.packageName}")
                    if (!shouldSaveNoRuleDiagnostic(payload, rules)) {
                        return@launch
                    }
                    saveDiagnostic(
                        payload,
                        NotificationDiagnostic.STAGE_NO_RULE_MATCH,
                        "no rule matched package=${payload.packageName}"
                    )
                    return@launch
                }
                val payment = PaymentAmountParser.parse(payload.rawText)
                if (payment == null) {
                    Log.d(TAG, "Skip notification because no amount was parsed")
                    saveDiagnostic(
                        payload,
                        NotificationDiagnostic.STAGE_NO_AMOUNT,
                        "matched rule ${rule.id} but no amount was parsed"
                    )
                    return@launch
                }
                val rawHash = Hashing.sha256(
                    listOf(payload.packageName, payload.notificationKey, payload.postedAt, payload.rawText)
                        .joinToString(separator = "|")
                )
                val inserted = container.paymentRepository.saveIfNew(payload, rule, payment, rawHash)
                if (inserted) {
                    val language = container.settingsStore.languageFlow.first()
                    Log.d(
                        TAG,
                        "Broadcast payment source=${rule.id} amount=${payment.currency}${payment.amount} ttsReady=${container.ttsManager.isReady()}"
                    )
                    val speechResult = container.ttsManager.speakPayment(rule, payment, language)
                    val diagnosticStage = when {
                        !speechResult.accepted -> NotificationDiagnostic.STAGE_TTS_FAILED
                        speechResult.queued -> NotificationDiagnostic.STAGE_TTS_QUEUED
                        else -> NotificationDiagnostic.STAGE_BROADCAST
                    }
                    saveDiagnostic(
                        payload,
                        diagnosticStage,
                        "broadcast ${rule.id} ${payment.currency}${payment.amount}; ${speechResult.message}"
                    )
                } else {
                    Log.d(TAG, "Skip notification because it is a duplicate")
                    saveDiagnostic(
                        payload,
                        NotificationDiagnostic.STAGE_DUPLICATE,
                        "duplicate of an earlier notification"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to process payment notification", error)
            }
        }
    }

    private fun shouldSaveNoRuleDiagnostic(
        payload: NotificationPayload,
        rules: List<com.hege.dingpay.data.PaymentRule>
    ): Boolean {
        if (PaymentAmountParser.parse(payload.rawText) != null) return true

        return rules
            .asSequence()
            .filter { it.enabled }
            .flatMap { it.keywords.asSequence() }
            .any { keyword -> payload.rawText.contains(keyword, ignoreCase = true) }
    }

    private suspend fun saveDiagnostic(
        payload: NotificationPayload,
        stage: String,
        message: String
    ) {
        val container = (application as DingPayApplication).container
        container.settingsStore.saveDiagnostic(
            NotificationDiagnostic(
                packageName = payload.packageName,
                stage = stage,
                message = message,
                rawTextPreview = NotificationDiagnostic.previewOf(payload.rawText),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override fun onDestroy() {
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val TAG = "DingPayListener"
        @Volatile
        private var activeInstance: WeakReference<PaymentNotificationListenerService>? = null

        fun requestActiveNotificationSync(): Boolean {
            val service = activeInstance?.get() ?: return false
            service.syncActiveNotifications("app_foreground")
            return true
        }
    }
}
