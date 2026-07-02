package com.hege.dingpay.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_records",
    indices = [
        Index(value = ["rawHash"], unique = true),
        Index(value = ["postedAt"])
    ]
)
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String,
    val sourceName: String,
    val amount: Double,
    val currency: String,
    val title: String,
    val text: String,
    val packageName: String,
    val notificationKey: String,
    val postedAt: Long,
    val createdAt: Long,
    val rawHash: String
)
