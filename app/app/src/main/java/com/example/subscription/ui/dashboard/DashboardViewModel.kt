package com.example.subscription.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.repository.SubscriptionRepository
import com.example.subscription.data.preferences.DashboardPreferencesRepository
import com.example.subscription.ui.dashboard.components.CardType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val prefsRepo: DashboardPreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val today = LocalDate.now()
                val currentYear = today.year
                val currentMonth = today.monthValue
                
                // 現在年月
                val aggregatesFlow = combine(
                    repository.getAllWithNextPayment(),
                    repository.getMonthlyTotal(currentYear, currentMonth),
                    repository.getNormalizedMonthlyTotal(),
                    repository.getRemainingThisMonthTotal(),
                    repository.getCostPerUseRanking()
                ) { subscriptions, monthlyTotal, normalizedMonthly, remainingThisMonth, costPerUseTop ->
                    Aggregates(
                        subscriptions = subscriptions,
                        monthlyTotal = monthlyTotal,
                        normalizedMonthly = normalizedMonthly,
                        remainingThisMonth = remainingThisMonth,
                        costPerUseTop = costPerUseTop
                    )
                }

                combine(
                    aggregatesFlow,
                    repository.getMonthlyBreakdown(currentYear, currentMonth),
                    prefsRepo.order,
                    prefsRepo.enabled
                ) { aggr, breakdown, order, enabled ->
                    val visibleOrder = order.filter { it in enabled }
                    DashboardUiState(
                        subscriptions = aggr.subscriptions,
                        monthlyTotal = aggr.monthlyTotal,
                        normalizedMonthlyTotal = aggr.normalizedMonthly,
                        remainingThisMonthTotal = aggr.remainingThisMonth,
                        costPerUseTop = aggr.costPerUseTop,
                        monthlyBreakdown = breakdown,
                        cardOrder = visibleOrder,
                        nextPayment = aggr.subscriptions.firstOrNull(),
                        isLoading = false,
                        error = null
                    )
                }.collect { newState -> _uiState.value = newState }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "エラーが発生しました",
                        isLoading = false
                    ) 
                }
            }
        }
    }

    private data class Aggregates(
        val subscriptions: List<com.example.subscription.data.model.SubscriptionWithPayment>,
        val monthlyTotal: Double,
        val normalizedMonthly: Double,
        val remainingThisMonth: Double,
        val costPerUseTop: List<com.example.subscription.data.model.CostPerUse>
    )
}
