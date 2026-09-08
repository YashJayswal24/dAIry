package com.yashjayswal.dairy.ui.theme

/** User-selectable theme preference. SYSTEM follows the device setting. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}
