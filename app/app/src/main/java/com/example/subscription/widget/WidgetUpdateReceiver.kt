package com.example.subscription.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WidgetUpdateReceiver", "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("WidgetUpdateReceiver", "System event detected, re-scheduling widget updates")
                // WorkManagerのスケジュールを再設定（停止していた場合に備える）
                WidgetUpdateScheduler.scheduleNextUpdate(context)
                // すべてのウィジェットを更新
                updateAllWidgets(context)
            }
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED -> {
                // バッテリーセーバー解除時などに対応
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isPowerSaveMode = powerManager.isPowerSaveMode
                
                Log.d("WidgetUpdateReceiver", "Power state changed, PowerSaveMode: $isPowerSaveMode")
                
                if (!isPowerSaveMode) {
                    // バッテリーセーバーが無効の場合、スケジュールを再設定
                    Log.d("WidgetUpdateReceiver", "PowerSaveMode disabled, re-scheduling")
                    WidgetUpdateScheduler.scheduleNextUpdate(context)
                    updateAllWidgets(context)
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // アプリ更新後
                Log.d("WidgetUpdateReceiver", "App updated, re-scheduling widget updates")
                WidgetUpdateScheduler.scheduleNextUpdate(context)
                updateAllWidgets(context)
            }
        }
    }
    
    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, SubscriptionWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        Log.d("WidgetUpdateReceiver", "Updating ${appWidgetIds.size} widgets")
        
        if (appWidgetIds.isNotEmpty()) {
            val updateIntent = Intent(context, SubscriptionWidgetProvider::class.java)
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(updateIntent)
        }
    }
}
