package com.example.subscription.data.local.dao

import androidx.room.*
import com.example.subscription.data.local.entity.ExchangeRate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rate")
    fun getAll(): Flow<List<ExchangeRate>>
    
    @Query("SELECT * FROM exchange_rate WHERE currency_code = :code")
    suspend fun getByCode(code: String): ExchangeRate?
    
    @Upsert
    suspend fun upsert(rate: ExchangeRate)
}
