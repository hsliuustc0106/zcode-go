package com.zcodego.app.data

enum class HostMode(val storageValue: String) {
    LOCAL("local"),
    REMOTE("remote"),
    ;

    companion object {
        fun fromStorage(value: String?): HostMode? =
            entries.firstOrNull { it.storageValue == value }
    }
}

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

data class AppSettings(
    val hostMode: HostMode? = null,
    val reopenLastSession: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastSessionId: String? = null,
    val secureRemoteScreen: Boolean = false,
)

/**
 * A saved remote-control link. [url] is a credential. [toString] omits it on purpose.
 */
data class RemoteSession(
    val id: String,
    val url: String,
    val sid: String,
    val deviceName: String,
    val appVersion: String?,
    val protocolLabel: String,
    val createdAtEpochMs: Long,
    val lastOpenedAtEpochMs: Long,
    val customDisplayName: String? = null,
) {
    override fun toString(): String = "RemoteSession(id=$id, protocol=$protocolLabel)"

    /** Local label. A blank custom name falls back to the name from the link. */
    fun visibleName(unnamedFallback: String): String =
        customDisplayName?.takeIf { it.isNotBlank() }
            ?: deviceName.takeIf { it.isNotBlank() }
            ?: unnamedFallback
}
