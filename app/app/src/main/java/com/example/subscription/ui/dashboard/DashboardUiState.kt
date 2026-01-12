package com.example.subscription.ui.dashboard

import com.example.subscription.data.model.SubscriptionWithPayment

data class DashboardUiState(
    val subscriptions: List<SubscriptionWithPayment> = emptyList(),
    val monthlyTotal: Double = 0.0,
    val nextPayment: SubscriptionWithPayment? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
