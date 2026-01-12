package com.example.subscription.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.example.subscription.MainActivity
import com.example.subscription.R
import com.example.subscription.data.local.AppDatabase
import com.example.subscription.data.repository.NextPaymentCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionWidgetProvider : AppWidgetProvider() {
    
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // WorkManagerのスケジュールを確認・再設定（停止している場合に備える）
        WidgetUpdateScheduler.scheduleNextUpdate(context)
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        // ウィジェットが最初に作成されたときの処理
        // 日次更新をスケジュール
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }
    
    override fun onDisabled(context: Context) {
        // 最後のウィジェットインスタンスが削除されたときの処理
        job.cancel()
        // スケジュールをキャンセル
        WidgetUpdateScheduler.cancelScheduledUpdate(context)
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            val views = RemoteViews(context.packageName, R.layout.subscription_widget)
            
            // アプリ起動用のインテント設定（タイトルクリック時）
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                appIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)
            
            // ウィジェット更新用のインテント設定（ウィジェット全体クリック時）
            val updateIntent = Intent(context, SubscriptionWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val updatePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // レイアウトのルート要素にリスナーを設定（widget_titleの親要素として機能）
            
            // データベースから次回支払い予定のサブスクリプションを取得
            try {
                val database = AppDatabase.getDatabase(context)
                val dao = database.subscriptionDao()
                
                // Flowではなく、suspendでブロッキング取得
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val subscriptions = dao.getAllActive().first()
                    
                    val calculator = NextPaymentCalculator()
                    
                    // 次回支払日を計算してソート
                    val subscriptionsWithPayment = subscriptions.map { subscription ->
                        val nextPaymentDate = calculator.calculate(
                            subscription.firstPaymentDate,
                            subscription.paymentCycle.name
                        )
                        val daysUntil = calculator.calculateDaysUntil(nextPaymentDate)
                        Triple(subscription, nextPaymentDate, daysUntil)
                    }.sortedBy { it.third }
                    
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        if (subscriptionsWithPayment.isEmpty()) {
                            // データがない場合
                            views.setTextViewText(R.id.widget_service_name, "登録なし")
                            views.setTextViewText(R.id.widget_days_until, "")
                            views.setTextViewText(R.id.widget_amount, "")
                            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_launcher_foreground)
                        } else {
                            // 直近の支払い予定を表示
                            val (subscription, _, daysUntil) = subscriptionsWithPayment.first()
                            
                            views.setTextViewText(R.id.widget_service_name, subscription.serviceName)
                            views.setTextViewText(R.id.widget_days_until, "あと${daysUntil}日")
                            
                            // 金額表示
                            val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
                            val amountText = if (subscription.currencyCode == "JPY") {
                                formatter.format(subscription.amount)
                            } else {
                                "${subscription.amount} ${subscription.currencyCode}"
                            }
                            views.setTextViewText(R.id.widget_amount, amountText)
                            
                            // アイコン表示
                            if (subscription.iconUrl != null && File(subscription.iconUrl).exists()) {
                                val bitmap = BitmapFactory.decodeFile(subscription.iconUrl)
                                views.setImageViewBitmap(R.id.widget_icon, bitmap)
                            } else {
                                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_launcher_foreground)
                            }
                        }
                        
                        // 更新時刻を表示
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        views.setTextViewText(R.id.widget_update_time, "更新: $currentTime")
                        
                        // ウィジェットを更新
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                views.setTextViewText(R.id.widget_service_name, "エラー")
                views.setTextViewText(R.id.widget_days_until, "")
                views.setTextViewText(R.id.widget_amount, e.message ?: "")
                
                // ウィジェットを更新
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
    
    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SubscriptionWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, SubscriptionWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                context.sendBroadcast(intent)
            }
        }
    }
}
