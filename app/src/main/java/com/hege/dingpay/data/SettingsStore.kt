package com.hege.dingpay.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hege.dingpay.license.LicenseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.dingPayDataStore by preferencesDataStore("dingpay_settings")

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.dingPayDataStore

    val languageFlow: Flow<BroadcastLanguage> = dataStore.data.map { prefs ->
        when (prefs[Keys.language]) {
            BroadcastLanguage.ES.name -> BroadcastLanguage.ES
            else -> BroadcastLanguage.ZH
        }
    }

    val serverUrlFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.serverUrl] ?: "http://10.0.2.2:8000"
    }

    val rulesFlow: Flow<List<PaymentRule>> = dataStore.data.map { prefs ->
        DefaultPaymentRules.rules.map { rule ->
            rule.copy(
                enabled = prefs[Keys.ruleEnabled(rule.id)] ?: rule.enabled,
                packageName = resolveRulePackage(rule, prefs[Keys.rulePackage(rule.id)]),
                keywords = resolveRuleKeywords(rule, prefs[Keys.ruleKeywords(rule.id)])
            )
        }
    }

    val licenseStateFlow: Flow<LicenseState> = combine(
        dataStore.data,
        serverUrlFlow
    ) { prefs, serverUrl ->
        LicenseState(
            licenseKey = prefs[Keys.licenseKey].orEmpty(),
            status = prefs[Keys.licenseStatus] ?: LicenseState.STATUS_INACTIVE,
            expiresAtMillis = prefs[Keys.licenseExpiresAt] ?: 0L,
            signedToken = prefs[Keys.signedToken].orEmpty(),
            offlineGraceDays = (prefs[Keys.offlineGraceDays] ?: 1L).toInt(),
            lastCheckedAtMillis = prefs[Keys.lastCheckedAt] ?: 0L,
            serverUrl = serverUrl
        )
    }

    val diagnosticFlow: Flow<NotificationDiagnostic> = dataStore.data.map { prefs ->
        val updatedAt = prefs[Keys.diagnosticUpdatedAt] ?: 0L
        if (updatedAt == 0L) {
            NotificationDiagnostic.EMPTY
        } else {
            NotificationDiagnostic(
                packageName = prefs[Keys.diagnosticPackage].orEmpty(),
                stage = prefs[Keys.diagnosticStage].orEmpty(),
                message = prefs[Keys.diagnosticMessage].orEmpty(),
                rawTextPreview = prefs[Keys.diagnosticPreview].orEmpty(),
                updatedAtMillis = updatedAt
            )
        }
    }

    suspend fun setLanguage(language: BroadcastLanguage) {
        dataStore.edit { prefs -> prefs[Keys.language] = language.name }
    }

    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ruleEnabled(ruleId)] = enabled }
    }

    suspend fun setRulePackage(ruleId: String, packageName: String) {
        dataStore.edit { prefs -> prefs[Keys.rulePackage(ruleId)] = packageName.trim() }
    }

    suspend fun setRuleKeywords(ruleId: String, keywords: List<String>) {
        dataStore.edit { prefs -> prefs[Keys.ruleKeywords(ruleId)] = joinRuleValues(keywords) }
    }

    suspend fun setServerUrl(serverUrl: String) {
        dataStore.edit { prefs -> prefs[Keys.serverUrl] = serverUrl.trim().trimEnd('/') }
    }

    suspend fun saveDiagnostic(diagnostic: NotificationDiagnostic) {
        dataStore.edit { prefs ->
            prefs[Keys.diagnosticPackage] = diagnostic.packageName
            prefs[Keys.diagnosticStage] = diagnostic.stage
            prefs[Keys.diagnosticMessage] = diagnostic.message
            prefs[Keys.diagnosticPreview] = diagnostic.rawTextPreview
            prefs[Keys.diagnosticUpdatedAt] = diagnostic.updatedAtMillis
        }
    }

    suspend fun saveLicense(state: LicenseState) {
        dataStore.edit { prefs ->
            prefs[Keys.licenseKey] = state.licenseKey
            prefs[Keys.licenseStatus] = state.status
            prefs[Keys.licenseExpiresAt] = state.expiresAtMillis
            prefs[Keys.signedToken] = state.signedToken
            prefs[Keys.offlineGraceDays] = state.offlineGraceDays.toLong()
            prefs[Keys.lastCheckedAt] = state.lastCheckedAtMillis
            prefs[Keys.serverUrl] = state.serverUrl.trim().trimEnd('/')
        }
    }

    private fun resolveRulePackage(rule: PaymentRule, storedPackage: String?): String {
        val normalizedPackage = storedPackage?.trim()
        if (
            rule.id == DefaultPaymentRules.EMAIL_RULE_ID &&
            normalizedPackage == DefaultPaymentRules.LEGACY_EMAIL_PACKAGE
        ) {
            return DefaultPaymentRules.NETEASE_EMAIL_PACKAGE
        }
        return normalizedPackage ?: rule.packageName
    }

    private fun resolveRuleKeywords(rule: PaymentRule, storedKeywords: String?): List<String> {
        val parsedKeywords = storedKeywords?.let(::splitRuleValues).orEmpty()
        return parsedKeywords.ifEmpty { rule.keywords }
    }

    private object Keys {
        val language = stringPreferencesKey("language")
        val serverUrl = stringPreferencesKey("server_url")
        val licenseKey = stringPreferencesKey("license_key")
        val licenseStatus = stringPreferencesKey("license_status")
        val licenseExpiresAt = longPreferencesKey("license_expires_at")
        val signedToken = stringPreferencesKey("signed_token")
        val offlineGraceDays = longPreferencesKey("offline_grace_days")
        val lastCheckedAt = longPreferencesKey("last_checked_at")

        val diagnosticPackage = stringPreferencesKey("diagnostic_package")
        val diagnosticStage = stringPreferencesKey("diagnostic_stage")
        val diagnosticMessage = stringPreferencesKey("diagnostic_message")
        val diagnosticPreview = stringPreferencesKey("diagnostic_preview")
        val diagnosticUpdatedAt = longPreferencesKey("diagnostic_updated_at")

        fun ruleEnabled(ruleId: String) = booleanPreferencesKey("rule_${ruleId}_enabled")
        fun rulePackage(ruleId: String) = stringPreferencesKey("rule_${ruleId}_package")
        fun ruleKeywords(ruleId: String) = stringPreferencesKey("rule_${ruleId}_keywords")
    }
}
