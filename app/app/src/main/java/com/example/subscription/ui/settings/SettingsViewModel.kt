package com.example.subscription.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.local.dao.ExchangeRateDao
import com.example.subscription.data.local.entity.ExchangeRate
import com.example.subscription.data.preferences.DashboardPreferencesRepository
import com.example.subscription.ui.dashboard.components.CardType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val prefsRepo: DashboardPreferencesRepository
) : ViewModel() {
    
    val rates: StateFlow<List<ExchangeRate>> = exchangeRateDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cardOrder: StateFlow<List<CardType>> = prefsRepo.order
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cardEnabled: StateFlow<Set<CardType>> = prefsRepo.enabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
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

    fun setCardVisible(type: CardType, visible: Boolean) {
        prefsRepo.setVisible(type, visible)
    }

    fun moveCardUp(type: CardType) {
        val current = cardOrder.value
        val idx = current.indexOf(type)
        if (idx > 0) {
            val new = current.toMutableList()
            new.removeAt(idx)
            new.add(idx - 1, type)
            prefsRepo.setOrder(new)
        }
    }

    fun moveCardDown(type: CardType) {
        val current = cardOrder.value
        val idx = current.indexOf(type)
        if (idx >= 0 && idx < current.size - 1) {
            val new = current.toMutableList()
            new.removeAt(idx)
            new.add(idx + 1, type)
            prefsRepo.setOrder(new)
        }
    }
}
