package com.yashjayswal.dairy.ui.theme

import android.content.Context

/** Persists the user's [ThemeMode] choice across app restarts. */
object ThemePreferences {
    private const val PREFS_NAME = "dairy_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun load(context: Context): ThemeMode {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }
}
