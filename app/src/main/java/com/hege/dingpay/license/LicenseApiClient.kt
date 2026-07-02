package com.hege.dingpay.license

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LicenseApiClient {
    fun activate(
        serverUrl: String,
        licenseKey: String,
        deviceId: String,
        appVersion: String
    ): LicenseState {
        val response = postJson(
            url = "$serverUrl/api/license/activate",
            body = JSONObject()
                .put("licenseKey", licenseKey)
                .put("deviceId", deviceId)
                .put("appVersion", appVersion)
        )
        return response.toLicenseState(serverUrl, licenseKey)
    }

    fun check(
        serverUrl: String,
        licenseKey: String,
        deviceId: String,
        signedToken: String,
        appVersion: String
    ): LicenseState {
        val response = postJson(
            url = "$serverUrl/api/license/check",
            body = JSONObject()
                .put("licenseKey", licenseKey)
                .put("deviceId", deviceId)
                .put("signedToken", signedToken)
                .put("appVersion", appVersion)
        )
        return response.toLicenseState(serverUrl, licenseKey)
    }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { stream ->
            stream.write(body.toString().toByteArray(Charsets.UTF_8))
        }
        val status = connection.responseCode
        val responseText = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (status !in 200..299) {
            val message = runCatching { JSONObject(responseText).optString("detail") }.getOrDefault("")
            throw IllegalStateException(message.ifBlank { "授权服务请求失败：HTTP $status" })
        }
        return JSONObject(responseText)
    }

    private fun JSONObject.toLicenseState(serverUrl: String, licenseKey: String): LicenseState {
        return LicenseState(
            licenseKey = optString("licenseKey", licenseKey),
            status = optString("status", LicenseState.STATUS_INACTIVE),
            expiresAtMillis = optLong("expiresAtMillis", 0L),
            signedToken = optString("signedToken", ""),
            offlineGraceDays = optInt("offlineGraceDays", 1),
            lastCheckedAtMillis = System.currentTimeMillis(),
            serverUrl = serverUrl
        )
    }
}
