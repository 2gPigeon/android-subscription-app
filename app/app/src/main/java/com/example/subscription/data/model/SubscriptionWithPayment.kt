package com.example.subscription.data.model

import com.example.subscription.data.local.entity.Subscription

data class SubscriptionWithPayment(
    val subscription: Subscription,
    val nextPaymentDate: String,
    val daysUntilPayment: Int
)
