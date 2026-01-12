package com.example.subscription.ui.addedit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.local.entity.Subscription
import com.example.subscription.data.model.PaymentCycle
import com.example.subscription.data.model.UsageFrequency
import com.example.subscription.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val subId: Int? = savedStateHandle.get<String>("subId")?.toIntOrNull()?.takeIf { it != -1 }
    
    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    
    init {
        subId?.let { loadSubscription(it) }
    }
    
    private fun loadSubscription(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val subscription = repository.getById(id)
                subscription?.let { sub ->
                    _uiState.update {
                        it.copy(
                            serviceName = sub.serviceName,
                            amount = sub.amount.toString(),
                            currencyCode = sub.currencyCode,
                            cycle = sub.paymentCycle,
                            firstPaymentDate = sub.firstPaymentDate,
                            frequency = UsageFrequency.fromInt(sub.usageFrequency),
                            note = sub.note ?: "",
                            iconUrl = sub.iconUrl,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun onServiceNameChanged(name: String) {
        _uiState.update { it.copy(serviceName = name) }
    }
    
    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }
    
    fun onCurrencyChanged(code: String) {
        _uiState.update { it.copy(currencyCode = code) }
    }
    
    fun onCycleChanged(cycle: PaymentCycle) {
        _uiState.update { it.copy(cycle = cycle) }
    }
    
    fun onDateChanged(date: String) {
        _uiState.update { it.copy(firstPaymentDate = date) }
    }
    
    fun onFrequencyChanged(frequency: UsageFrequency) {
        _uiState.update { it.copy(frequency = frequency) }
    }
    
    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }
    
    fun onIconSelected(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(iconUrl = null) }
            return
        }
        
        viewModelScope.launch {
            try {
                val savedPath = saveIconToInternalStorage(uri)
                _uiState.update { it.copy(iconUrl = savedPath) }
            } catch (e: Exception) {
                // エラーハンドリング（必要に応じてUIに通知）
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun saveIconToInternalStorage(uri: Uri): String = withContext(Dispatchers.IO) {
        val iconDir = File(context.filesDir, "subscription_icons")
        if (!iconDir.exists()) {
            iconDir.mkdirs()
        }
        
        val timestamp = System.currentTimeMillis()
        val fileName = "icon_$timestamp.jpg"
        val destFile = File(iconDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        
        destFile.absolutePath
    }
    
    fun saveSubscription() {
        if (!validateInput()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val subscription = Subscription(
                    id = subId ?: 0,
                    serviceName = _uiState.value.serviceName.trim(),
                    amount = _uiState.value.amount.toDouble(),
                    currencyCode = _uiState.value.currencyCode,
                    paymentCycle = _uiState.value.cycle,
                    firstPaymentDate = _uiState.value.firstPaymentDate,
                    usageFrequency = _uiState.value.frequency.toInt(),
                    note = _uiState.value.note.trim().ifBlank { null },
                    iconUrl = _uiState.value.iconUrl
                )
                
                if (subId == null) {
                    repository.insert(subscription)
                } else {
                    repository.update(subscription)
                }
                
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errors = mapOf("general" to (e.message ?: "保存に失敗しました"))
                    ) 
                }
            }
        }
    }
    
    private fun validateInput(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value
        
        if (state.serviceName.isBlank()) {
            errors["serviceName"] = "サービス名を入力してください"
        }
        
        val amountValue = state.amount.toDoubleOrNull()
        if (amountValue == null) {
            errors["amount"] = "有効な金額を入力してください"
        } else if (amountValue < 0) {
            errors["amount"] = "0以上の金額を入力してください"
        }
        
        // 日付フォーマットの検証（簡易的）
        if (!state.firstPaymentDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            errors["firstPaymentDate"] = "有効な日付を選択してください"
        }
        
        _uiState.update { it.copy(errors = errors) }
        return errors.isEmpty()
    }
}
