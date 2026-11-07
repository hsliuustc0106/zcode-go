package com.zcodego.app.data

/**
 * Forgets the chosen host mode so the first-run picker shows again.
 * Sessions, last-session id, theme, and reopen stay on the settings object.
 */
internal object HostModeReset {
    fun withoutChoice(settings: AppSettings): AppSettings = settings.copy(hostMode = null)

    fun showsPicker(settings: AppSettings): Boolean = settings.hostMode == null
}
