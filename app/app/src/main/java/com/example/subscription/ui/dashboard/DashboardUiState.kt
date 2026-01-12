package com.example.subscription.ui.dashboard

import com.example.subscription.data.model.SubscriptionWithPayment
import com.example.subscription.data.model.CostPerUse
import com.example.subscription.data.model.MonthlyBreakdownItem
import com.example.subscription.ui.dashboard.components.CardType

data class DashboardUiState(
    val subscriptions: List<SubscriptionWithPayment> = emptyList(),
    val monthlyTotal: Double = 0.0,
    val normalizedMonthlyTotal: Double = 0.0,
    val remainingThisMonthTotal: Double = 0.0,
    val costPerUseTop: List<CostPerUse> = emptyList(),
    val monthlyBreakdown: List<MonthlyBreakdownItem> = emptyList(),
    val cardOrder: List<CardType> = emptyList(),
    val nextPayment: SubscriptionWithPayment? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
