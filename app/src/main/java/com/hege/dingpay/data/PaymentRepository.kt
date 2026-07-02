package com.hege.dingpay.data

import kotlinx.coroutines.flow.Flow

class PaymentRepository(private val dao: PaymentRecordDao) {
    fun observeAll(): Flow<List<PaymentRecordEntity>> = dao.observeAll()

    fun observeRecent(days: Int = 7): Flow<List<PaymentRecordEntity>> {
        val since = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        return dao.observeSince(since)
    }

    suspend fun saveIfNew(
        payload: NotificationPayload,
        rule: PaymentRule,
        payment: ParsedPayment,
        rawHash: String
    ): Boolean {
        val insertedId = dao.insertIgnore(
            PaymentRecordEntity(
                sourceType = rule.sourceType.name,
                sourceName = rule.title,
                amount = payment.amount,
                currency = payment.currency,
                title = payload.title,
                text = payload.text,
                packageName = payload.packageName,
                notificationKey = payload.notificationKey,
                postedAt = payload.postedAt,
                createdAt = System.currentTimeMillis(),
                rawHash = rawHash
            )
        )
        return insertedId > 0
    }

    suspend fun clear() {
        dao.clear()
    }
}
