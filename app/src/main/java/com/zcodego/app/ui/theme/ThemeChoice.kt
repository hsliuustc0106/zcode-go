package com.zcodego.app.ui.theme

import com.zcodego.app.data.ThemeMode

/** Default is the system appearance. Light and dark are explicit overrides. */
internal object ThemeChoice {
    const val DYNAMIC_COLOR_MIN_SDK = 31

    fun useDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    /** Wallpaper colors only while following the system, and only where the platform has them. */
    fun useDynamicColor(mode: ThemeMode, sdkInt: Int): Boolean =
        mode == ThemeMode.SYSTEM && sdkInt >= DYNAMIC_COLOR_MIN_SDK
}
