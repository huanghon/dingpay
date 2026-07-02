package com.hege.dingpay.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PaymentRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paymentRecordDao(): PaymentRecordDao
}
