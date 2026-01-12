package com.example.subscription.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.subscription.data.local.entity.ExchangeRate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val rates by viewModel.rates.collectAsState()
    var showRateDialog by remember { mutableStateOf<ExchangeRate?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "為替レート設定",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "各通貨の対円レートを設定します",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(rates) { rate ->
                ExchangeRateCard(
                    rate = rate,
                    onClick = { showRateDialog = rate }
                )
            }
        }
    }
    
    // レート編集ダイアログ
    showRateDialog?.let { rate ->
        RateEditDialog(
            rate = rate,
            onDismiss = { showRateDialog = null },
            onConfirm = { newRate ->
                viewModel.updateRate(rate.currencyCode, newRate)
                showRateDialog = null
            }
        )
    }
}

@Composable
private fun ExchangeRateCard(
    rate: ExchangeRate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = rate.currencyCode,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = getCurrencyName(rate.currencyCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = "¥${String.format("%.2f", rate.rateToJpy)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "1 ${rate.currencyCode}あたり",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RateEditDialog(
    rate: ExchangeRate,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var inputValue by remember { mutableStateOf(rate.rateToJpy.toString()) }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = rate.currencyCode,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        title = { 
            Text("為替レートを入力") 
        },
        text = {
            Column {
                Text(
                    text = "1 ${rate.currencyCode} = ? 円",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { 
                        inputValue = it
                        isError = it.toDoubleOrNull() == null || it.toDoubleOrNull()!! <= 0
                    },
                    label = { Text("レート") },
                    placeholder = { Text("150.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("0より大きい数値を入力してください")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    inputValue.toDoubleOrNull()?.let { newRate ->
                        if (newRate > 0) {
                            onConfirm(newRate)
                        }
                    }
                },
                enabled = !isError && inputValue.toDoubleOrNull() != null
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

private fun getCurrencyName(code: String): String {
    return when (code) {
        "USD" -> "米ドル"
        "EUR" -> "ユーロ"
        "GBP" -> "英ポンド"
        "CNY" -> "中国元"
        "KRW" -> "韓国ウォン"
        else -> ""
    }
}
