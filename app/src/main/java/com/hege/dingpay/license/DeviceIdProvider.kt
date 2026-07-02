package com.hege.dingpay.license

import android.content.Context
import android.provider.Settings
import java.util.UUID

class DeviceIdProvider(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("dingpay_device", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val installId = prefs.getString(KEY_INSTALL_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALL_ID, it).apply()
        }
        return "${androidId.ifBlank { "unknown" }}-$installId"
    }

    private companion object {
        const val KEY_INSTALL_ID = "install_id"
    }
}
