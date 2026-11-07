package com.zcodego.app.data

import com.zcodego.app.security.RejectReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds one incoming remote URL (deep link, share, or clipboard) until the UI consumes it.
 * The URL is a credential and is not written to logs.
 */
class LinkInbox {
    private val acceptedState = MutableStateFlow<String?>(null)
    val accepted: StateFlow<String?> = acceptedState.asStateFlow()

    private val rejectedState = MutableStateFlow<LinkRejection?>(null)
    val rejected: StateFlow<LinkRejection?> = rejectedState.asStateFlow()

    fun offerAccepted(url: String) {
        acceptedState.value = url
    }

    fun offerRejected(reason: RejectReason, entered: String = "") {
        rejectedState.value = LinkRejection(reason, entered.trim().take(MAX_ENTERED))
    }

    fun peekAccepted(): String? = acceptedState.value

    fun consumeAccepted(): String? {
        val current = acceptedState.value
        acceptedState.value = null
        return current
    }

    fun consumeRejected(): LinkRejection? {
        val current = rejectedState.value
        rejectedState.value = null
        return current
    }

    private companion object {
        const val MAX_ENTERED = 200
    }
}

/** The text the user entered. It is shown in the paste field and not written to logs. */
data class LinkRejection(val reason: RejectReason, val entered: String)
