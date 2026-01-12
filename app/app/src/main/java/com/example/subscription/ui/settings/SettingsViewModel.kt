package com.example.subscription.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.local.dao.ExchangeRateDao
import com.example.subscription.data.local.entity.ExchangeRate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao
) : ViewModel() {
    
    val rates: StateFlow<List<ExchangeRate>> = exchangeRateDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun updateRate(code: String, rate: Double) {
        viewModelScope.launch {
            val exchangeRate = ExchangeRate(
                currencyCode = code,
                rateToJpy = rate,
                updatedAt = System.currentTimeMillis()
            )
            exchangeRateDao.upsert(exchangeRate)
        }
    }
}
