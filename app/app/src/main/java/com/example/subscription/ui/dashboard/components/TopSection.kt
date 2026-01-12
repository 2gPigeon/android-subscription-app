package com.example.subscription.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.example.subscription.data.model.SubscriptionWithPayment
import com.example.subscription.data.model.CostPerUse
import com.example.subscription.data.model.MonthlyBreakdownItem
import com.example.subscription.ui.dashboard.components.CardType
import java.text.NumberFormat
import java.util.*

@Composable
fun TopSection(
    nextPayment: SubscriptionWithPayment?,
    monthlyTotal: Double,
    normalizedMonthlyTotal: Double,
    remainingThisMonthTotal: Double,
    costPerUseTop: List<CostPerUse> = emptyList(),
    monthlyBreakdown: List<MonthlyBreakdownItem> = emptyList(),
    cardOrder: List<CardType> = emptyList(),
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { cardOrder.size.coerceAtLeast(1) })
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // ページャー
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) { page ->
            if (cardOrder.isEmpty()) {
                MonthlyTotalCard(monthlyTotal)
            } else {
                when (cardOrder[page]) {
                    CardType.NEXT_PAYMENT -> NextPaymentCard(nextPayment)
                    CardType.MONTHLY_TOTAL -> MonthlyTotalCard(monthlyTotal)
                    CardType.NORMALIZED_MONTHLY -> NormalizedMonthlyCard(normalizedMonthlyTotal)
                    CardType.REMAINING_THIS_MONTH -> RemainingThisMonthCard(remainingThisMonthTotal)
                    CardType.COST_PER_USE -> CostPerUseCard(costPerUseTop)
                    CardType.MONTHLY_PIE -> MonthlyPieCard(monthlyBreakdown)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ページインジケーター
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val indicatorCount = cardOrder.size.coerceAtLeast(1)
            repeat(indicatorCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                Color.Gray.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
}

@Composable
private fun NextPaymentCard(nextPayment: SubscriptionWithPayment?) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (nextPayment != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "次回支払い",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nextPayment.subscription.serviceName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextPayment.nextPaymentDate,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 残り日数に応じて色を変更
                    val daysColor = when {
                        nextPayment.daysUntilPayment <= 3 -> Color.Red
                        nextPayment.daysUntilPayment <= 7 -> Color(0xFFFF9800) // Orange
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Text(
                        text = "あと${nextPayment.daysUntilPayment}日",
                        style = MaterialTheme.typography.titleLarge,
                        color = daysColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "サブスクリプションがありません",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlyTotalCard(monthlyTotal: Double) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
    
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "今月の支出",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatter.format(monthlyTotal),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NormalizedMonthlyCard(total: Double) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "月額換算 合計",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatter.format(total),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RemainingThisMonthCard(total: Double) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "今月の残り支払い",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatter.format(total),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CostPerUseCard(items: List<CostPerUse>) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "割高ランキング（1回あたり）",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "データがありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items.take(3).forEach { cpu ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cpu.serviceName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        val textValue = if (cpu.costPerUseJpy.isInfinite()) "∞" else formatter.format(cpu.costPerUseJpy)
                        Text(
                            text = "$textValue / 回",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyPieCard(items: List<MonthlyBreakdownItem>) {
    val total = items.sumOf { it.amountJpy }
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (total <= 0.0 || items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "今月の支払いはありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Card
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Pie chart
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                PieChart(items)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Legend
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items) { item ->
                    val percent = if (total == 0.0) 0.0 else (item.amountJpy / total * 100.0)
                    LegendRow(label = item.serviceName, percent = percent)
                }
            }
        }
    }
}

@Composable
private fun PieChart(items: List<MonthlyBreakdownItem>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        Color(0xFFEC407A),
        Color(0xFFAB47BC),
        Color(0xFF29B6F6),
        Color(0xFF26A69A),
        Color(0xFFFFB300),
        Color(0xFFFF7043)
    )
    val total = items.sumOf { it.amountJpy }
    Canvas(modifier = Modifier.size(140.dp)) {
        var startAngle = -90f
        items.forEachIndexed { index, item ->
            val sweep = if (total == 0.0) 0f else ((item.amountJpy / total) * 360f).toFloat()
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                size = Size(size.minDimension, size.minDimension),
                topLeft = Offset((size.width - size.minDimension) / 2f, (size.height - size.minDimension) / 2f)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendRow(label: String, percent: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
        Text(
            text = String.format("%.1f%%", percent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
