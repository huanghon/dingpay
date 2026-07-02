package com.hege.dingpay.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.hege.dingpay.data.NotificationPayload

object NotificationTextExtractor {
    fun extract(sbn: StatusBarNotification): NotificationPayload? {
        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(separator = "\n") { it.toString() }
            .orEmpty()
        val raw = listOf(title, text, bigText, lines)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
            .trim()
        if (raw.isBlank()) return null
        return NotificationPayload(
            packageName = sbn.packageName,
            notificationKey = sbn.key,
            postedAt = sbn.postTime,
            title = title,
            text = listOf(text, bigText, lines).firstOrNull { it.isNotBlank() }.orEmpty(),
            rawText = raw
        )
    }
}
