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
import com.example.subscription.ui.dashboard.components.CardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val rates by viewModel.rates.collectAsState()
    val cardOrder by viewModel.cardOrder.collectAsState()
    val cardEnabled by viewModel.cardEnabled.collectAsState()
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
            // Dashboard card settings
            item {
                Text(
                    text = "ダッシュボード カード設定",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "表示カードの選択と表示順を変更",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(cardOrder) { type ->
                CardRow(
                    type = type,
                    enabled = cardEnabled.contains(type),
                    canMoveUp = cardOrder.indexOf(type) > 0,
                    canMoveDown = cardOrder.indexOf(type) < cardOrder.size - 1,
                    onToggle = { viewModel.setCardVisible(type, it) },
                    onMoveUp = { viewModel.moveCardUp(type) },
                    onMoveDown = { viewModel.moveCardDown(type) }
                )
            }

            // Exchange rate settings
            item {
                Text(
                    text = "為替レート設定",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "通貨の対JPYレートを設定します",
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

    // Rate edit dialog
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
private fun CardRow(
    type: CardType,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = cardTitle(type), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = cardDescription(type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("↑") }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("↓") }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
        }
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
                    text = "1 ${rate.currencyCode} あたり",
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
        title = { Text("為替レートを入力") },
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
                        if (newRate > 0) onConfirm(newRate)
                    }
                },
                enabled = !isError && inputValue.toDoubleOrNull() != null
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

private fun cardTitle(type: CardType): String = when (type) {
    CardType.NEXT_PAYMENT -> "次回支払い"
    CardType.MONTHLY_TOTAL -> "今月の支出"
    CardType.NORMALIZED_MONTHLY -> "月額換算 合計"
    CardType.REMAINING_THIS_MONTH -> "今月の残り支払い"
    CardType.COST_PER_USE -> "割高ランキング（1回あたり）"
    CardType.MONTHLY_PIE -> "今月の内訳（円グラフ）"
}

private fun cardDescription(type: CardType): String = when (type) {
    CardType.NEXT_PAYMENT -> "直近の支払日と残日数"
    CardType.MONTHLY_TOTAL -> "当月に発生する支出の合計"
    CardType.NORMALIZED_MONTHLY -> "全サブスクを月額に正規化した合計"
    CardType.REMAINING_THIS_MONTH -> "今日以降〜月末までの予定支払合計"
    CardType.COST_PER_USE -> "月額÷利用頻度で割高な順に表示"
    CardType.MONTHLY_PIE -> "当月支払いのサービス別比率"
}

private fun getCurrencyName(code: String): String {
    return when (code) {
        "USD" -> "米ドル"
        "EUR" -> "ユーロ"
        "GBP" -> "英ポンド"
        "CNY" -> "中国人民元"
        "KRW" -> "韓国ウォン"
        else -> ""
    }
}

