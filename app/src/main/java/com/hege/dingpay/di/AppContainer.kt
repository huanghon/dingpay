package com.hege.dingpay.di

import android.content.Context
import androidx.room.Room
import com.hege.dingpay.data.AppDatabase
import com.hege.dingpay.data.PaymentRepository
import com.hege.dingpay.data.SettingsStore
import com.hege.dingpay.license.DeviceIdProvider
import com.hege.dingpay.license.LicenseApiClient
import com.hege.dingpay.license.LicenseRepository
import com.hege.dingpay.service.TtsManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "dingpay.db"
    ).fallbackToDestructiveMigration(true).build()

    val settingsStore = SettingsStore(appContext)
    val paymentRepository = PaymentRepository(database.paymentRecordDao())
    val deviceIdProvider = DeviceIdProvider(appContext)
    val licenseApiClient = LicenseApiClient()
    val licenseRepository = LicenseRepository(
        settingsStore = settingsStore,
        deviceIdProvider = deviceIdProvider,
        apiClient = licenseApiClient
    )
    val ttsManager = TtsManager(appContext)
}
