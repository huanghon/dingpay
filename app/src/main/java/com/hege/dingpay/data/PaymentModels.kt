package com.hege.dingpay.data

enum class PaymentSourceType {
    BANK,
    YAPPY,
    EMAIL
}

enum class BroadcastLanguage {
    ZH,
    ES
}

data class PaymentRule(
    val id: String,
    val sourceType: PaymentSourceType,
    val title: String,
    val description: String,
    val packageName: String,
    val keywords: List<String>,
    val enabled: Boolean
) {
    val packageNames: List<String>
        get() = splitRuleValues(packageName)
}

fun splitRuleValues(raw: String): List<String> {
    return raw
        .split(",", "，", "\n", ";", "；")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

fun joinRuleValues(values: List<String>): String {
    return values
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
}

object DefaultPaymentRules {
    const val EMAIL_RULE_ID = "email"
    const val LEGACY_EMAIL_PACKAGE = "com.android.email"
    const val NETEASE_EMAIL_PACKAGE = "com.netease.mobimail"

    val rules = listOf(
        PaymentRule(
            id = "bank",
            sourceType = PaymentSourceType.BANK,
            title = "银行到账",
            description = "监听银行类应用收款通知",
            packageName = "com.bgeneral",
            keywords = listOf("recibiste", "recibido", "envio", "pago", "detalle", "te envio"),
            enabled = true
        ),
        PaymentRule(
            id = "yappy",
            sourceType = PaymentSourceType.YAPPY,
            title = "Yappy到账",
            description = "监听 Yappy 支付收款通知",
            packageName = "com.yappy",
            keywords = listOf("Yappy", "yappy", "recibiste", "envio", "pago", "detalle", "te envio", "por Yappy"),
            enabled = true
        ),
        PaymentRule(
            id = EMAIL_RULE_ID,
            sourceType = PaymentSourceType.EMAIL,
            title = "邮件到账",
            description = "监听系统邮箱收款通知",
            packageName = NETEASE_EMAIL_PACKAGE,
            keywords = listOf("recibiste", "recibido", "B/.", "de"),
            enabled = true
        )
    )
}

data class ParsedPayment(
    val amount: Double,
    val currency: String
)

data class NotificationPayload(
    val packageName: String,
    val notificationKey: String,
    val postedAt: Long,
    val title: String,
    val text: String,
    val rawText: String
)

/**
 * Snapshot of the most recent notification the listener processed. Persisted
 * locally so users can diagnose why a real notification did or did not get
 * broadcast without having to read logcat.
 */
data class NotificationDiagnostic(
    val packageName: String,
    val stage: String,
    val message: String,
    val rawTextPreview: String,
    val updatedAtMillis: Long
) {
    companion object {
        const val STAGE_LICENSE_INACTIVE = "license_inactive"
        const val STAGE_NO_RULE_MATCH = "no_rule_match"
        const val STAGE_NO_AMOUNT = "no_amount"
        const val STAGE_DUPLICATE = "duplicate"
        const val STAGE_BROADCAST = "broadcast"
        const val STAGE_TTS_QUEUED = "tts_queued"
        const val STAGE_TTS_FAILED = "tts_failed"

        const val PREVIEW_LIMIT = 200

        val EMPTY = NotificationDiagnostic(
            packageName = "",
            stage = "",
            message = "",
            rawTextPreview = "",
            updatedAtMillis = 0L
        )

        fun previewOf(rawText: String): String {
            if (rawText.length <= PREVIEW_LIMIT) return rawText
            return rawText.take(PREVIEW_LIMIT) + "…"
        }
    }
}
