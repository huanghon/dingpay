package com.hege.dingpay.license

import com.hege.dingpay.BuildConfig
import com.hege.dingpay.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LicenseRepository(
    private val settingsStore: SettingsStore,
    private val deviceIdProvider: DeviceIdProvider,
    private val apiClient: LicenseApiClient
) {
    val stateFlow: Flow<LicenseState> = settingsStore.licenseStateFlow

    fun deviceId(): String = deviceIdProvider.getDeviceId()

    suspend fun activate(licenseKey: String, serverUrl: String): LicenseState = withContext(Dispatchers.IO) {
        val state = apiClient.activate(
            serverUrl = serverUrl.trim().trimEnd('/'),
            licenseKey = licenseKey.trim(),
            deviceId = deviceId(),
            appVersion = BuildConfig.VERSION_NAME
        )
        settingsStore.saveLicense(state)
        state
    }

    suspend fun checkNow(): LicenseState = withContext(Dispatchers.IO) {
        val current = stateFlow.first()
        if (current.licenseKey.isBlank() || current.signedToken.isBlank()) return@withContext current
        val checked = apiClient.check(
            serverUrl = current.serverUrl,
            licenseKey = current.licenseKey,
            deviceId = deviceId(),
            signedToken = current.signedToken,
            appVersion = BuildConfig.VERSION_NAME
        )
        settingsStore.saveLicense(checked)
        checked
    }

    suspend fun isBroadcastAllowed(): Boolean {
        return stateFlow.first().isActive
    }
}
