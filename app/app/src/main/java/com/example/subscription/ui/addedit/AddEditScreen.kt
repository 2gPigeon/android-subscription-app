package com.example.subscription.ui.addedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.subscription.data.model.PaymentCycle
import com.example.subscription.data.model.UsageFrequency
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    navController: NavController,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // 保存完了時の処理
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("サブスクリプション登録") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // サービス名入力
                OutlinedTextField(
                    value = uiState.serviceName,
                    onValueChange = { viewModel.onServiceNameChanged(it) },
                    label = { Text("サービス名 *") },
                    isError = uiState.errors.containsKey("serviceName"),
                    supportingText = {
                        uiState.errors["serviceName"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 金額入力
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    label = { Text("金額 *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.errors.containsKey("amount"),
                    supportingText = {
                        uiState.errors["amount"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 通貨選択
                CurrencySelector(
                    selectedCurrency = uiState.currencyCode,
                    onCurrencyChanged = { viewModel.onCurrencyChanged(it) }
                )
                
                // アイコン選択
                IconSelector(
                    iconUrl = uiState.iconUrl,
                    serviceName = uiState.serviceName,
                    onIconSelected = { uri -> viewModel.onIconSelected(uri) }
                )
                
                // 支払いサイクル選択
                PaymentCycleSelector(
                    selectedCycle = uiState.cycle,
                    onCycleChanged = { viewModel.onCycleChanged(it) }
                )
                
                // 初回支払日選択
                DateSelector(
                    selectedDate = uiState.firstPaymentDate,
                    onDateChanged = { viewModel.onDateChanged(it) }
                )
                
                // 利用頻度選択
                UsageFrequencySelector(
                    selectedFrequency = uiState.frequency,
                    onFrequencyChanged = { viewModel.onFrequencyChanged(it) }
                )
                
                // メモ入力
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    label = { Text("メモ（任意）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
                
                // 一般エラー表示
                uiState.errors["general"]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 保存ボタン
                Button(
                    onClick = { viewModel.saveSubscription() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("保存")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelector(
    selectedCurrency: String,
    onCurrencyChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = listOf("JPY", "USD", "EUR")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            label = { Text("通貨") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencyChanged(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun IconSelector(
    iconUrl: String?,
    serviceName: String,
    onIconSelected: (Uri?) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onIconSelected(uri)
    }
    
    Column {
        Text(
            text = "アイコン（任意）",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコンプレビュー
            Card(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconUrl != null && File(iconUrl).exists()) {
                        AsyncImage(
                            model = File(iconUrl),
                            contentDescription = "サービスアイコン",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // ボタン群
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("画像を選択")
                }
                
                if (iconUrl != null) {
                    OutlinedButton(
                        onClick = { onIconSelected(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("削除")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentCycleSelector(
    selectedCycle: PaymentCycle,
    onCycleChanged: (PaymentCycle) -> Unit
) {
    Column {
        Text(
            text = "支払いサイクル",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaymentCycle.values().forEach { cycle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCycleChanged(cycle) },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCycle == cycle,
                        onClick = { onCycleChanged(cycle) }
                    )
                    Text(
                        text = when (cycle) {
                            PaymentCycle.MONTHLY -> "月ごと"
                            PaymentCycle.BIANNUALLY -> "半年ごと"
                            PaymentCycle.YEARLY -> "年ごと"
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(
    selectedDate: String,
    onDateChanged: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            val date = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
            date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )
    
    OutlinedTextField(
        value = selectedDate,
        onValueChange = {},
        label = { Text("初回支払日") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, "日付選択")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true }
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = java.time.Instant.ofEpochMilli(millis)
                            val date = java.time.LocalDate.ofInstant(
                                instant,
                                java.time.ZoneId.systemDefault()
                            )
                            onDateChanged(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "初回支払日を選択",
                        modifier = Modifier.padding(16.dp)
                    )
                },
                showModeToggle = true
            )
        }
    }
}

@Composable
private fun UsageFrequencySelector(
    selectedFrequency: UsageFrequency,
    onFrequencyChanged: (UsageFrequency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "利用頻度",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (selectedFrequency) {
                    UsageFrequency.DAILY -> "毎日（月30回）"
                    UsageFrequency.WEEKDAY -> "平日（月22回）"
                    UsageFrequency.WEEKLY_2_3 -> "週2-3回（月10回）"
                    UsageFrequency.WEEKLY -> "週1回（月4回）"
                    UsageFrequency.MONTHLY -> "月1回"
                    UsageFrequency.RARELY -> "ほぼ使わない"
                }
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            UsageFrequency.values().forEach { freq ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (freq) {
                                UsageFrequency.DAILY -> "毎日（月30回）"
                                UsageFrequency.WEEKDAY -> "平日（月22回）"
                                UsageFrequency.WEEKLY_2_3 -> "週2-3回（月10回）"
                                UsageFrequency.WEEKLY -> "週1回（月4回）"
                                UsageFrequency.MONTHLY -> "月1回"
                                UsageFrequency.RARELY -> "ほぼ使わない"
                            }
                        )
                    },
                    onClick = {
                        onFrequencyChanged(freq)
                        expanded = false
                    }
                )
            }
        }
    }
}
