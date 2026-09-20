package com.simple.bulletjournal.data

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class UserPreferences(
    val isAdRemoved: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
