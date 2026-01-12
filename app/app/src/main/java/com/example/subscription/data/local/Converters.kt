package com.example.subscription.data.local

import androidx.room.TypeConverter
import com.example.subscription.data.model.PaymentCycle

class Converters {
    @TypeConverter
    fun fromPaymentCycle(value: PaymentCycle): String {
        return value.name
    }
    
    @TypeConverter
    fun toPaymentCycle(value: String): PaymentCycle {
        return PaymentCycle.valueOf(value)
    }
}
