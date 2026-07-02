package com.hege.dingpay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payment_records ORDER BY postedAt DESC, id DESC")
    fun observeAll(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE postedAt >= :since ORDER BY postedAt DESC, id DESC")
    fun observeSince(since: Long): Flow<List<PaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(record: PaymentRecordEntity): Long

    @Query("DELETE FROM payment_records")
    suspend fun clear()
}
