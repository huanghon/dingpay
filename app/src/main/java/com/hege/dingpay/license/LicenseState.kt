package com.hege.dingpay.license

data class LicenseState(
    val licenseKey: String = "",
    val status: String = STATUS_INACTIVE,
    val expiresAtMillis: Long = 0L,
    val signedToken: String = "",
    val offlineGraceDays: Int = 1,
    val lastCheckedAtMillis: Long = 0L,
    val serverUrl: String = "http://10.0.2.2:8000"
) {
    val isPermanent: Boolean
        get() = status == STATUS_ACTIVE && expiresAtMillis <= 0L

    val isActive: Boolean
        get() = status == STATUS_ACTIVE && (isPermanent || expiresAtMillis > System.currentTimeMillis())

    val remainingDays: Long
        get() {
            if (expiresAtMillis <= 0L) return 0L
            val remaining = expiresAtMillis - System.currentTimeMillis()
            return if (remaining <= 0L) 0L else (remaining + DAY_MILLIS - 1) / DAY_MILLIS
        }

    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_INACTIVE = "inactive"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_DISABLED = "disabled"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
