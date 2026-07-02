package com.hege.dingpay.service

import com.hege.dingpay.data.NotificationPayload
import com.hege.dingpay.data.PaymentRule

object PaymentRuleMatcher {
    /**
     * Match a notification against the enabled rules.
     *
     * Empty [PaymentRule.packageNames] is treated as "no package restriction" so
     * users can broadcast for apps whose true package we do not know yet.
     *
     * Empty [PaymentRule.keywords] is treated as "no keyword restriction". In
     * that case we require [PaymentAmountParser] to actually pull out an
     * amount, otherwise we would broadcast on unrelated notifications.
     */
    fun match(payload: NotificationPayload, rules: List<PaymentRule>): PaymentRule? {
        return rules.firstOrNull { rule ->
            if (!rule.enabled) return@firstOrNull false

            val packageOk = rule.packageNames.isEmpty() ||
                rule.packageNames.any { packageName ->
                    payload.packageName.equals(packageName, ignoreCase = true)
                }
            if (!packageOk) return@firstOrNull false

            if (rule.keywords.isEmpty()) {
                // Fallback: require a parseable amount when keywords are absent
                // so we do not broadcast for every notification.
                return@firstOrNull PaymentAmountParser.parse(payload.rawText) != null
            }

            rule.keywords.any { keyword ->
                payload.rawText.contains(keyword, ignoreCase = true)
            }
        }
    }
}
