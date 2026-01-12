package com.example.subscription.data.local.dao

import androidx.room.*
import com.example.subscription.data.local.entity.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription WHERE is_active = 1 ORDER BY first_payment_date ASC")
    fun getAllActiveSortedByDate(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscription WHERE is_active = 1")
    fun getAllActive(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscription WHERE id = :id")
    suspend fun getById(id: Int): Subscription?
    
    @Insert
    suspend fun insert(subscription: Subscription): Long
    
    @Update
    suspend fun update(subscription: Subscription)
    
    @Query("UPDATE subscription SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivate(id: Int, timestamp: Long)
}
