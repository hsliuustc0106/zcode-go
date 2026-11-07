package com.zcodego.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First-launch smoke test. It needs a device or emulator; unit tests do not run it.
 */
@RunWith(AndroidJUnit4::class)
class HostPickerSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstLaunchShowsHostPicker() {
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("ZCode 在哪里运行?").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("ZCode 在哪里运行?").assertIsDisplayed()
        rule.onNodeWithText("本机").assertIsDisplayed()
        rule.onNodeWithText("远程主机").assertIsDisplayed()
        rule.onNodeWithText("继续").assertIsDisplayed()
    }
}
