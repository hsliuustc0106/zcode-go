package com.zcodego.app.ui.theme

import com.zcodego.app.data.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeChoiceTest {
    @Test
    fun systemFollowsThePlatformAndOverridesDoNot() {
        assertTrue(ThemeChoice.useDark(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(ThemeChoice.useDark(ThemeMode.SYSTEM, systemDark = false))
        assertFalse(ThemeChoice.useDark(ThemeMode.LIGHT, systemDark = true))
        assertTrue(ThemeChoice.useDark(ThemeMode.DARK, systemDark = false))
    }

    @Test
    fun dynamicColorOnlyFollowsSystemOnAndroid12() {
        assertTrue(ThemeChoice.useDynamicColor(ThemeMode.SYSTEM, sdkInt = 31))
        assertTrue(ThemeChoice.useDynamicColor(ThemeMode.SYSTEM, sdkInt = 36))
        assertFalse(ThemeChoice.useDynamicColor(ThemeMode.SYSTEM, sdkInt = 30))
        assertFalse(ThemeChoice.useDynamicColor(ThemeMode.LIGHT, sdkInt = 34))
        assertFalse(ThemeChoice.useDynamicColor(ThemeMode.DARK, sdkInt = 34))
    }
}
