package com.zcodego.app.ui.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectOnceTest {
    @Test
    fun networkReturnDuringReloadableFailureFiresOnce() {
        var state = ReconnectOnce.State()
        var step = ReconnectOnce.step(state, online = false, reloadableFailure = true)
        state = step.state
        assertFalse(step.reconnect)

        step = ReconnectOnce.step(state, online = true, reloadableFailure = true)
        state = step.state
        assertTrue(step.reconnect)

        step = ReconnectOnce.step(state, online = true, reloadableFailure = true)
        state = step.state
        assertFalse(step.reconnect)

        step = ReconnectOnce.step(state, online = false, reloadableFailure = true)
        state = step.state
        assertFalse(step.reconnect)
        step = ReconnectOnce.step(state, online = true, reloadableFailure = true)
        assertTrue(step.reconnect)
    }

    @Test
    fun onlineFailureWithoutOfflineDoesNotFire() {
        val step = ReconnectOnce.step(ReconnectOnce.State(), online = true, reloadableFailure = true)
        assertFalse(step.reconnect)
        assertFalse(step.state.armed)
    }

    @Test
    fun certificateFailureDoesNotConsumeTheShot() {
        val armed = ReconnectOnce.step(ReconnectOnce.State(), online = false, reloadableFailure = false).state
        val step = ReconnectOnce.step(armed, online = true, reloadableFailure = false)
        assertFalse(step.reconnect)
        assertFalse(step.state.armed)
    }
}
