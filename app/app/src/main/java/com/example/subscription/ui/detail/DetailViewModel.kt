package com.example.subscription.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.local.dao.ExchangeRateDao
import com.example.subscription.data.model.SubscriptionWithPayment
import com.example.subscription.data.repository.NextPaymentCalculator
import com.example.subscription.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val calculator: NextPaymentCalculator,
    private val exchangeRateDao: ExchangeRateDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val subId: Int = savedStateHandle.get<String>("subId")?.toIntOrNull() ?: 0
    
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    
    init {
        loadSubscription(subId)
    }
    
    private fun loadSubscription(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val subscription = repository.getById(id)
                
                if (subscription == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "サブスクリプションが見つかりません"
                        ) 
                    }
                    return@launch
                }
                
                // 次回支払日計算
                val nextPaymentDate = calculator.calculate(
                    subscription.firstPaymentDate,
                    subscription.paymentCycle.name
                )
                
                val daysUntil = calculator.calculateDaysUntil(nextPaymentDate)
                
                // 為替レート取得
                val exchangeRate = exchangeRateDao.getByCode(subscription.currencyCode)
                val rateToJpy = exchangeRate?.rateToJpy ?: 1.0
                
                // 1回あたりコスト計算（円換算）
                val monthlyAmount = when (subscription.paymentCycle.name) {
                    "MONTHLY" -> subscription.amount * rateToJpy
                    "BIANNUALLY" -> (subscription.amount * rateToJpy) / 6.0
                    "YEARLY" -> (subscription.amount * rateToJpy) / 12.0
                    else -> subscription.amount * rateToJpy
                }
                
                val costPerUse = if (subscription.usageFrequency > 0) {
                    monthlyAmount / subscription.usageFrequency.toDouble()
                } else {
                    0.0
                }
                
                val subscriptionWithPayment = SubscriptionWithPayment(
                    subscription = subscription,
                    nextPaymentDate = nextPaymentDate,
                    daysUntilPayment = daysUntil
                )
                
                _uiState.update {
                    it.copy(
                        subscription = subscriptionWithPayment,
                        costPerUse = costPerUse,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "エラーが発生しました"
                    ) 
                }
            }
        }
    }
    
    fun deleteSubscription() {
        viewModelScope.launch {
            try {
                repository.delete(subId)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "削除に失敗しました") 
                }
            }
        }
    }
}
