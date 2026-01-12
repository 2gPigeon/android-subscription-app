package com.example.subscription.data.model

enum class UsageFrequency(private val value: Int) {
    DAILY(30),
    WEEKDAY(22),
    WEEKLY_2_3(10),
    WEEKLY(4),
    MONTHLY(1),
    RARELY(0);

    fun toInt(): Int = value

    companion object {
        fun fromInt(value: Int): UsageFrequency {
            return when (value) {
                30 -> DAILY
                22 -> WEEKDAY
                10 -> WEEKLY_2_3
                4 -> WEEKLY
                1 -> MONTHLY
                0 -> RARELY
                else -> MONTHLY // デフォルト値
            }
        }
    }
}
