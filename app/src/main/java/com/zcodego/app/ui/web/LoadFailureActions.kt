package com.zcodego.app.ui.web

internal sealed interface FatalWebError {
    data object Ssl : FatalWebError
    data object Load : FatalWebError
    data object Threat : FatalWebError
    data object Unavailable : FatalWebError
}

/**
 * A saved remote session can replace a dead link. Official pages have no session to delete.
 * Only a failed load is treated as a stale address. Certificate and threat failures stay put.
 */
internal object LoadFailureActions {
    fun offersSessionRecovery(error: FatalWebError, hasSavedSession: Boolean): Boolean =
        hasSavedSession && error == FatalWebError.Load
}
