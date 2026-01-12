package com.example.subscription.data.repository

import android.content.Context
import com.example.subscription.data.local.dao.ExchangeRateDao
import com.example.subscription.data.local.dao.SubscriptionDao
import com.example.subscription.data.local.entity.Subscription
import com.example.subscription.data.model.SubscriptionWithPayment
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
