package com.example.subscription.ui.addedit

import com.example.subscription.data.model.PaymentCycle
import com.example.subscription.data.model.UsageFrequency
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AddEditUiState(
    val serviceName: String = "",
    val amount: String = "",
    val currencyCode: String = "JPY",
    val cycle: PaymentCycle = PaymentCycle.MONTHLY,
    val firstPaymentDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val frequency: UsageFrequency = UsageFrequency.MONTHLY,
    val note: String = "",
    val iconUrl: String? = null,
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)
