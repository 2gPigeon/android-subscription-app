package com.example.subscription.data.repository

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class NextPaymentCalculator @Inject constructor() {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    /**
     * 次回支払日を計算
     * @param firstPaymentDate 初回支払日 (YYYY-MM-DD形式)
     * @param paymentCycle 支払いサイクル ("MONTHLY", "BIANNUALLY" or "YEARLY")
     * @return 次回支払日 (YYYY-MM-DD形式)
     */
    fun calculate(firstPaymentDate: String, paymentCycle: String): String {
        val firstDate = LocalDate.parse(firstPaymentDate, dateFormatter)
        val today = LocalDate.now()
        
        var nextPaymentDate = firstDate
        var loopCount = 0
        val maxLoops = 1000 // 無限ループ防止
        
        // 今日を超えるまで支払いサイクルを加算
        while (nextPaymentDate.isBefore(today) || nextPaymentDate.isEqual(today)) {
            if (loopCount++ > maxLoops) {
                throw IllegalStateException("次回支払日の計算でループ上限に達しました")
            }
            
            nextPaymentDate = when (paymentCycle) {
                "MONTHLY" -> nextPaymentDate.plusMonths(1)
                "BIANNUALLY" -> nextPaymentDate.plusMonths(6)
                "YEARLY" -> nextPaymentDate.plusYears(1)
                else -> throw IllegalArgumentException("無効な支払いサイクル: $paymentCycle")
            }
            
            // 月末処理: 計算結果が月の日数を超える場合は月末に丸める
            nextPaymentDate = clampToMonthEnd(nextPaymentDate)
        }
        
        return nextPaymentDate.format(dateFormatter)
    }
    
    /**
     * 日付が月末を超える場合、月末に丸める
     * RepositoryからもアクセスできるようにInternal公開
     */
    fun clampToMonthEnd(date: LocalDate): LocalDate {
        val lastDayOfMonth = date.month.length(date.isLeapYear)
        return if (date.dayOfMonth > lastDayOfMonth) {
            date.withDayOfMonth(lastDayOfMonth)
        } else {
            date
        }
    }
    
    /**
     * 次回支払日までの残り日数を計算
     */
    fun calculateDaysUntil(nextPaymentDate: String): Int {
        val nextDate = LocalDate.parse(nextPaymentDate, dateFormatter)
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, nextDate).toInt()
    }
}
