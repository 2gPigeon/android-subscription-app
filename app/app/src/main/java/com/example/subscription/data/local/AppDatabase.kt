package com.example.subscription.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.subscription.data.local.dao.ExchangeRateDao
import com.example.subscription.data.local.dao.SubscriptionDao
import com.example.subscription.data.local.entity.ExchangeRate
import com.example.subscription.data.local.entity.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Subscription::class, ExchangeRate::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "subscription_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 初期データの挿入
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateDatabase(database.exchangeRateDao())
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDatabase(exchangeRateDao: ExchangeRateDao) {
            // 初期為替レートデータの挿入
            exchangeRateDao.upsert(
                ExchangeRate(
                    currencyCode = "USD",
                    rateToJpy = 150.0,
                    updatedAt = System.currentTimeMillis()
                )
            )
            exchangeRateDao.upsert(
                ExchangeRate(
                    currencyCode = "JPY",
                    rateToJpy = 1.0,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
