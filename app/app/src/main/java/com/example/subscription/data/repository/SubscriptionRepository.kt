package com.example.subscription.data.repository

import android.content.Context
import com.example.subscription.data.local.dao.ExchangeRateDao
import com.example.subscription.data.local.dao.SubscriptionDao
import com.example.subscription.data.local.entity.Subscription
import com.example.subscription.data.model.CostPerUse
import com.example.subscription.data.model.SubscriptionWithPayment
import com.example.subscription.data.model.MonthlyBreakdownItem
import com.example.subscription.widget.SubscriptionWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val calculator: NextPaymentCalculator,
    @ApplicationContext private val context: Context
) {
    
    /**
     * すべてのアクティブなサブスクリプションを次回支払日付きで取得
     */
    fun getAllWithNextPayment(): Flow<List<SubscriptionWithPayment>> {
        return subscriptionDao.getAllActiveSortedByDate().map { subscriptions ->
            subscriptions.map { subscription ->
                val nextPaymentDate = calculator.calculate(
                    subscription.firstPaymentDate,
                    subscription.paymentCycle.name
                )
                val daysUntil = calculator.calculateDaysUntil(nextPaymentDate)
                
                SubscriptionWithPayment(
                    subscription = subscription,
                    nextPaymentDate = nextPaymentDate,
                    daysUntilPayment = daysUntil
                )
            }.sortedBy { it.nextPaymentDate }
        }
    }

    /**
     * 月額換算(JPY) / 利用頻度 で割高ランキングを返す（降順）
     * usage_frequency が 0 の場合は非常に割高とみなし、無限大相当で扱う
     */
    fun getCostPerUseRanking(limit: Int = 3): Flow<List<CostPerUse>> {
        return combine(
            subscriptionDao.getAllActive(),
            exchangeRateDao.getAll()
        ) { subscriptions, exchangeRates ->
            val rateMap = exchangeRates.associateBy { it.currencyCode }
            subscriptions.map { s ->
                val rate = rateMap[s.currencyCode]?.rateToJpy ?: 1.0
                val monthlyFactor = when (s.paymentCycle.name) {
                    "MONTHLY" -> 1.0
                    "BIANNUALLY" -> 1.0 / 6.0
                    "YEARLY" -> 1.0 / 12.0
                    else -> 1.0
                }
                val monthlyJpy = s.amount * rate * monthlyFactor
                val freq = s.usageFrequency
                val cpu = if (freq <= 0) Double.POSITIVE_INFINITY else monthlyJpy / freq.toDouble()
                CostPerUse(serviceName = s.serviceName, costPerUseJpy = cpu)
            }
                .sortedByDescending { it.costPerUseJpy }
                .take(limit)
        }
    }

    /**
     * すべてのアクティブなサブスクを月額換算し、JPYに換算して合計を返す
     * 年払いは12で割り、半年払いは6で割る
     */
    fun getNormalizedMonthlyTotal(): Flow<Double> {
        return combine(
            subscriptionDao.getAllActive(),
            exchangeRateDao.getAll()
        ) { subscriptions, exchangeRates ->
            val rateMap = exchangeRates.associateBy { it.currencyCode }
            subscriptions.sumOf { subscription ->
                val rate = rateMap[subscription.currencyCode]?.rateToJpy ?: 1.0
                val monthlyFactor = when (subscription.paymentCycle.name) {
                    "MONTHLY" -> 1.0
                    "BIANNUALLY" -> 1.0 / 6.0
                    "YEARLY" -> 1.0 / 12.0
                    else -> 1.0
                }
                subscription.amount * rate * monthlyFactor
            }
        }
    }

    /**
     * 本日から当月末までに到来する支払いの合計（JPY換算）
     */
    fun getRemainingThisMonthTotal(): Flow<Double> {
        val today = LocalDate.now()
        val endOfMonth = YearMonth.of(today.year, today.month).atEndOfMonth()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        return combine(
            getAllWithNextPayment(),
            exchangeRateDao.getAll()
        ) { subsWithPayment, exchangeRates ->
            val rateMap = exchangeRates.associateBy { it.currencyCode }
            subsWithPayment.sumOf { item ->
                val nextDate = LocalDate.parse(item.nextPaymentDate, formatter)
                if (!nextDate.isBefore(today) && !nextDate.isAfter(endOfMonth)) {
                    val rate = rateMap[item.subscription.currencyCode]?.rateToJpy ?: 1.0
                    item.subscription.amount * rate
                } else 0.0
            }
        }
    }

    /**
     * 指定年月の支払い内訳（JPY）を返す
     */
    fun getMonthlyBreakdown(year: Int, month: Int): Flow<List<MonthlyBreakdownItem>> {
        return combine(
            subscriptionDao.getAllActive(),
            exchangeRateDao.getAll()
        ) { subscriptions, exchangeRates ->
            val rateMap = exchangeRates.associateBy { it.currencyCode }
            val targetMonth = YearMonth.of(year, month)
            val targetStart = targetMonth.atDay(1)
            val targetEnd = targetMonth.atEndOfMonth()

            subscriptions.mapNotNull { subscription ->
                val firstPaymentDate = LocalDate.parse(
                    subscription.firstPaymentDate,
                    DateTimeFormatter.ISO_LOCAL_DATE
                )
                if (firstPaymentDate.isAfter(targetEnd)) {
                    return@mapNotNull null
                }

                var currentPaymentDate = firstPaymentDate
                while (currentPaymentDate.isBefore(targetStart)) {
                    currentPaymentDate = when (subscription.paymentCycle.name) {
                        "MONTHLY" -> currentPaymentDate.plusMonths(1)
                        "BIANNUALLY" -> currentPaymentDate.plusMonths(6)
                        "YEARLY" -> currentPaymentDate.plusYears(1)
                        else -> break
                    }
                    currentPaymentDate = calculator.clampToMonthEnd(currentPaymentDate)
                }

                if (!currentPaymentDate.isAfter(targetEnd) && !currentPaymentDate.isBefore(targetStart)) {
                    val rate = rateMap[subscription.currencyCode]?.rateToJpy ?: 1.0
                    MonthlyBreakdownItem(
                        serviceName = subscription.serviceName,
                        amountJpy = subscription.amount * rate
                    )
                } else null
            }.sortedByDescending { it.amountJpy }
        }
    }

    /**
     * 指定月の支払い合計を円換算で取得
     * 初回支払日と支払いサイクルから、指定月に支払いが発生するかを判定
     */
    fun getMonthlyTotal(year: Int, month: Int): Flow<Double> {
        return combine(
            subscriptionDao.getAllActive(),
            exchangeRateDao.getAll()
        ) { subscriptions, exchangeRates ->
            val rateMap = exchangeRates.associateBy { it.currencyCode }
            val targetMonth = YearMonth.of(year, month)
            val targetStart = targetMonth.atDay(1)
            val targetEnd = targetMonth.atEndOfMonth()
            
            subscriptions.sumOf { subscription ->
                // 初回支払日から指定月までの間に支払いが発生するか判定
                val firstPaymentDate = LocalDate.parse(
                    subscription.firstPaymentDate,
                    DateTimeFormatter.ISO_LOCAL_DATE
                )
                
                // 初回支払日が対象月より後の場合はスキップ
                if (firstPaymentDate.isAfter(targetEnd)) {
                    return@sumOf 0.0
                }
                
                // 支払いサイクルに応じて、対象月内に支払いが発生するか判定
                var currentPaymentDate = firstPaymentDate
                var hasPaymentInMonth = false
                
                // 対象月の開始日まで支払日を進める
                while (currentPaymentDate.isBefore(targetStart)) {
                    currentPaymentDate = when (subscription.paymentCycle.name) {
                        "MONTHLY" -> currentPaymentDate.plusMonths(1)
                        "BIANNUALLY" -> currentPaymentDate.plusMonths(6)
                        "YEARLY" -> currentPaymentDate.plusYears(1)
                        else -> break
                    }
                    // 月末補正
                    currentPaymentDate = calculator.clampToMonthEnd(currentPaymentDate)
                }
                
                // 対象月内に支払いがあるかチェック
                if (!currentPaymentDate.isAfter(targetEnd) && 
                    !currentPaymentDate.isBefore(targetStart)) {
                    hasPaymentInMonth = true
                }
                
                if (hasPaymentInMonth) {
                    val rate = rateMap[subscription.currencyCode]?.rateToJpy ?: 1.0
                    subscription.amount * rate
                } else {
                    0.0
                }
            }
        }
    }
    
    /**
     * IDでサブスクリプションを取得
     */
    suspend fun getById(id: Int): Subscription? {
        return subscriptionDao.getById(id)
    }
    
    /**
     * サブスクリプションを新規登録
     */
    suspend fun insert(subscription: Subscription): Long {
        val now = System.currentTimeMillis()
        val result = subscriptionDao.insert(
            subscription.copy(
                createdAt = now,
                updatedAt = now
            )
        )
        // ウィジェットを更新
        SubscriptionWidgetProvider.updateAllWidgets(context)
        return result
    }
    
    /**
     * サブスクリプションを更新
     */
    suspend fun update(subscription: Subscription) {
        subscriptionDao.update(
            subscription.copy(
                updatedAt = System.currentTimeMillis()
            )
        )
        // ウィジェットを更新
        SubscriptionWidgetProvider.updateAllWidgets(context)
    }
    
    /**
     * サブスクリプションを削除（論理削除）
     */
    suspend fun delete(id: Int) {
        subscriptionDao.deactivate(id, System.currentTimeMillis())
        // ウィジェットを更新
        SubscriptionWidgetProvider.updateAllWidgets(context)
    }
}
