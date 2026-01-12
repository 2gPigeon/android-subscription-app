package com.example.subscription.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.subscription.ui.dashboard.components.CardType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)

    private val defaultOrder = listOf(
        CardType.NEXT_PAYMENT,
        CardType.MONTHLY_TOTAL,
        CardType.NORMALIZED_MONTHLY,
        CardType.REMAINING_THIS_MONTH,
        CardType.COST_PER_USE,
        CardType.MONTHLY_PIE
    )

    private val _order = MutableStateFlow(loadOrder())
    val order: StateFlow<List<CardType>> = _order.asStateFlow()

    private val _enabled = MutableStateFlow(loadEnabled())
    val enabled: StateFlow<Set<CardType>> = _enabled.asStateFlow()

    private fun loadOrder(): List<CardType> {
        val raw = prefs.getString(KEY_ORDER, null) ?: return defaultOrder
        return raw.split(',')
            .mapNotNull { runCatching { CardType.valueOf(it) }.getOrNull() }
            .ifEmpty { defaultOrder }
    }

    private fun loadEnabled(): Set<CardType> {
        val set = mutableSetOf<CardType>()
        for (type in CardType.values()) {
            val key = KEY_VISIBLE_PREFIX + type.name
            val visible = prefs.getBoolean(key, true)
            if (visible) set.add(type)
        }
        return if (set.isEmpty()) CardType.values().toSet() else set
    }

    fun setOrder(newOrder: List<CardType>) {
        val value = newOrder.joinToString(",") { it.name }
        prefs.edit().putString(KEY_ORDER, value).apply()
        _order.value = newOrder
    }

    fun setVisible(type: CardType, visible: Boolean) {
        prefs.edit().putBoolean(KEY_VISIBLE_PREFIX + type.name, visible).apply()
        _enabled.value = loadEnabled()
    }

    companion object {
        private const val KEY_ORDER = "cards_order"
        private const val KEY_VISIBLE_PREFIX = "card_visible_"
    }
}

