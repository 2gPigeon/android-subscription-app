package com.example.subscription.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.subscription.data.model.PaymentCycle

@Entity(tableName = "subscription")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "service_name")
    val serviceName: String,
    
    @ColumnInfo(name = "amount")
    val amount: Double,
    
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    
    @ColumnInfo(name = "payment_cycle")
    val paymentCycle: PaymentCycle,
    
    @ColumnInfo(name = "first_payment_date")
    val firstPaymentDate: String,
    
    @ColumnInfo(name = "usage_frequency")
    val usageFrequency: Int,
    
    @ColumnInfo(name = "note")
    val note: String? = null,
    
    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Int = 1,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L
)
