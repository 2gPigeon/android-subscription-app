package com.example.subscription.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.subscription.data.model.UsageFrequency
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // 削除完了時の処理
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(uiState.subscription?.subscription?.serviceName ?: "詳細") 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    if (uiState.subscription != null) {
                        IconButton(
                            onClick = { 
                                navController.navigate("add_edit?subId=${uiState.subscription?.subscription?.id}")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "編集"
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "エラーが発生しました",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                uiState.subscription != null -> {
                    DetailContent(
                        subscription = uiState.subscription!!,
                        costPerUse = uiState.costPerUse
                    )
                }
            }
        }
    }
    
    // 削除確認ダイアログ
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("削除確認") },
            text = { 
                Text("${uiState.subscription?.subscription?.serviceName} を削除しますか？\n\nこの操作は取り消せません。") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubscription()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun DetailContent(
    subscription: com.example.subscription.data.model.SubscriptionWithPayment,
    costPerUse: Double
) {
    val formatter = NumberFormat.getInstance(Locale.getDefault())
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ヘッダーカード
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subscription.subscription.serviceName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${formatter.format(subscription.subscription.amount)} ${subscription.subscription.currencyCode}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // 基本情報セクション
        SectionCard(title = "基本情報") {
            InfoRow(
                label = "支払いサイクル",
                value = when (subscription.subscription.paymentCycle.name) {
                    "MONTHLY" -> "月ごと"
                    "BIANNUALLY" -> "半年ごと"
                    "YEARLY" -> "年ごと"
                    else -> subscription.subscription.paymentCycle.name
                }
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(
                label = "通貨",
                value = subscription.subscription.currencyCode
            )
        }
        
        // スケジュール情報セクション
        SectionCard(title = "スケジュール") {
            InfoRow(
                label = "初回支払日",
                value = subscription.subscription.firstPaymentDate
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(
                label = "次回支払日",
                value = subscription.nextPaymentDate
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(
                label = "支払いまで",
                value = "あと${subscription.daysUntilPayment}日"
            )
        }
        
        // 利用状況セクション
        SectionCard(title = "利用状況") {
            val frequencyText = when (UsageFrequency.fromInt(subscription.subscription.usageFrequency)) {
                UsageFrequency.DAILY -> "毎日（月30回）"
                UsageFrequency.WEEKDAY -> "平日（月22回）"
                UsageFrequency.WEEKLY_2_3 -> "週2-3回（月10回）"
                UsageFrequency.WEEKLY -> "週1回（月4回）"
                UsageFrequency.MONTHLY -> "月1回"
                UsageFrequency.RARELY -> "ほぼ使わない"
            }
            
            InfoRow(
                label = "利用頻度",
                value = frequencyText
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(
                label = "1回あたりコスト",
                value = if (costPerUse > 0) currencyFormatter.format(costPerUse) else "計算不可"
            )
        }
        
        // メモセクション
        subscription.subscription.note?.let { note ->
            if (note.isNotBlank()) {
                SectionCard(title = "メモ") {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
