package com.example.subscription.ui.detail

import com.example.subscription.data.model.SubscriptionWithPayment

data class DetailUiState(
    val subscription: SubscriptionWithPayment? = null,
    val costPerUse: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDeleted: Boolean = false
)
