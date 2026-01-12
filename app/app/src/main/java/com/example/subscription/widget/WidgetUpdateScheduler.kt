package com.example.subscription.widget

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {
    
    private const val WORK_NAME = "widget_daily_update"
    
    fun scheduleNextUpdate(context: Context) {
        // 制約を緩く設定（バッテリー最適化の影響を受けにくくする）
        val constraints = Constraints.Builder()
            // バッテリー低下時でも実行を許可（明示的に設定）
            .setRequiresBatteryNotLow(false)  // バッテリーセーバー時でも実行
            .setRequiresCharging(false)        // 充電不要
            .setRequiresDeviceIdle(false)      // デバイスアイドル不要
            .build()
        
        // 15分ごとに実行されるワーカーをスケジュール（最小間隔）
        // ※本番環境では1日に変更してください
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            15, TimeUnit.MINUTES  // テスト用: 15分ごと（最小値）
            // 1, TimeUnit.DAYS  // 本番用: 1日ごと
        )
            .setConstraints(constraints)
            .build()
        
        Log.d("WidgetUpdateScheduler", "Scheduling widget update work with relaxed constraints")
        
        // UPDATEに変更：既存のワークを置き換えて確実に再スケジュール
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,  // KEEPではなくUPDATEを使用
            workRequest
        )
    }
    
    fun cancelScheduledUpdate(context: Context) {
        Log.d("WidgetUpdateScheduler", "Cancelling widget update work")
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
