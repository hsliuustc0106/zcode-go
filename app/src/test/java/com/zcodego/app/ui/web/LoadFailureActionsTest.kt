package com.zcodego.app.ui.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadFailureActionsTest {
    @Test
    fun savedSessionLoadOffersRescanAndDelete() {
        assertTrue(LoadFailureActions.offersSessionRecovery(FatalWebError.Load, hasSavedSession = true))
    }

    @Test
    fun officialPageAndOtherFailuresDoNot() {
        assertFalse(LoadFailureActions.offersSessionRecovery(FatalWebError.Load, hasSavedSession = false))
        assertFalse(LoadFailureActions.offersSessionRecovery(FatalWebError.Ssl, hasSavedSession = true))
        assertFalse(LoadFailureActions.offersSessionRecovery(FatalWebError.Threat, hasSavedSession = true))
        assertFalse(LoadFailureActions.offersSessionRecovery(FatalWebError.Unavailable, hasSavedSession = true))
    }
}
