package com.example.subscription.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.repository.SubscriptionRepository
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
    private val repository: SubscriptionRepository
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
                
                // サブスクリプション一覧と月間合計を同時に取得
                combine(
                    repository.getAllWithNextPayment(),
                    repository.getMonthlyTotal(currentYear, currentMonth)
                ) { subscriptions, monthlyTotal ->
                    DashboardUiState(
                        subscriptions = subscriptions,
                        monthlyTotal = monthlyTotal,
                        nextPayment = subscriptions.firstOrNull(), // 次回支払いは先頭の要素
                        isLoading = false,
                        error = null
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
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
}
