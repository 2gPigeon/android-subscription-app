package com.example.subscription.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rate")
data class ExchangeRate(
    @PrimaryKey
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    
    @ColumnInfo(name = "rate_to_jpy")
    val rateToJpy: Double,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
