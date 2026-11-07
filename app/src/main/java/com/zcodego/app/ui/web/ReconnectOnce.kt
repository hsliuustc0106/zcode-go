package com.zcodego.app.ui.web

/**
 * One automatic reload when connectivity returns during a load or disconnect failure.
 * An offline sample arms a single shot. The next online sample fires only when the
 * visible failure is reloadable, then the shot stays spent until offline is seen again.
 */
internal object ReconnectOnce {
    data class State(val armed: Boolean = false)

    data class Step(val state: State, val reconnect: Boolean)

    class Holder(var state: State = State())

    fun step(state: State, online: Boolean, reloadableFailure: Boolean): Step {
        if (!online) return Step(State(armed = true), reconnect = false)
        if (state.armed && reloadableFailure) return Step(State(armed = false), reconnect = true)
        return Step(State(armed = false), reconnect = false)
    }
}
