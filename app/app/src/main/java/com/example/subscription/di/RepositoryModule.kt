package com.example.subscription.di

import android.content.Context
import com.example.subscription.data.local.dao.ExchangeRateDao
import com.example.subscription.data.local.dao.SubscriptionDao
import com.example.subscription.data.repository.NextPaymentCalculator
import com.example.subscription.data.repository.SubscriptionRepository
import com.example.subscription.data.preferences.DashboardPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideNextPaymentCalculator(): NextPaymentCalculator {
        return NextPaymentCalculator()
    }
    
    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        subscriptionDao: SubscriptionDao,
        exchangeRateDao: ExchangeRateDao,
        calculator: NextPaymentCalculator,
        @ApplicationContext context: Context
    ): SubscriptionRepository {
        return SubscriptionRepository(subscriptionDao, exchangeRateDao, calculator, context)
    }

    @Provides
    @Singleton
    fun provideDashboardPreferencesRepository(
        @ApplicationContext context: Context
    ): DashboardPreferencesRepository {
        return DashboardPreferencesRepository(context)
    }
}
