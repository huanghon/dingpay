package com.hege.dingpay.service

import com.hege.dingpay.data.DefaultPaymentRules
import com.hege.dingpay.data.NotificationPayload
import com.hege.dingpay.data.joinRuleValues
import com.hege.dingpay.data.splitRuleValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PaymentRuleMatcherTest {
    @Test
    fun matches_netease_mail_payment_notification() {
        val payload = NotificationPayload(
            packageName = DefaultPaymentRules.NETEASE_EMAIL_PACKAGE,
            notificationKey = "test-key",
            postedAt = 1L,
            title = "recibiste B/. 1.25 de Cliente Prueba",
            text = "recibiste B/. 1.25 de Cliente Prueba",
            rawText = "recibiste B/. 1.25 de Cliente Prueba"
        )

        val rule = PaymentRuleMatcher.match(payload, DefaultPaymentRules.rules)

        assertNotNull(rule)
        assertEquals(DefaultPaymentRules.EMAIL_RULE_ID, rule!!.id)
    }

    @Test
    fun matches_any_configured_package_for_rule() {
        val emailRule = DefaultPaymentRules.rules
            .first { it.id == DefaultPaymentRules.EMAIL_RULE_ID }
            .copy(packageName = "com.android.email, ${DefaultPaymentRules.NETEASE_EMAIL_PACKAGE}")
        val payload = NotificationPayload(
            packageName = DefaultPaymentRules.NETEASE_EMAIL_PACKAGE,
            notificationKey = "test-key",
            postedAt = 1L,
            title = "recibiste B/. 1.25 de Cliente Prueba",
            text = "recibiste B/. 1.25 de Cliente Prueba",
            rawText = "recibiste B/. 1.25 de Cliente Prueba"
        )

        val rule = PaymentRuleMatcher.match(payload, listOf(emailRule))

        assertNotNull(rule)
        assertEquals(DefaultPaymentRules.EMAIL_RULE_ID, rule!!.id)
    }

    @Test
    fun parses_custom_rule_values_from_common_separators() {
        val values = splitRuleValues("recibiste, pago\nB/.；Yappy，detalle")

        assertEquals("recibiste, pago, B/., Yappy, detalle", joinRuleValues(values))
    }
}
