package com.example.subscription.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WidgetUpdateWorker", "Starting widget update at ${System.currentTimeMillis()}")
            
            // すべてのウィジェットを更新
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SubscriptionWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            Log.d("WidgetUpdateWorker", "Found ${appWidgetIds.size} widgets to update")
            
            if (appWidgetIds.isNotEmpty()) {
                SubscriptionWidgetProvider().onUpdate(context, appWidgetManager, appWidgetIds)
                Log.d("WidgetUpdateWorker", "Widget update completed successfully")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("WidgetUpdateWorker", "Widget update failed", e)
            e.printStackTrace()
            // 失敗時は再試行（最大3回まで自動リトライ）
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
