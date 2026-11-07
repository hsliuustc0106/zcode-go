package com.zcodego.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zcodego.app.data.SessionRestore
import com.zcodego.app.data.ThemeMode
import com.zcodego.app.navigation.AppNav
import com.zcodego.app.remote.RemoteIngest
import com.zcodego.app.security.ParseResult
import com.zcodego.app.security.RemoteUrlPolicy
import com.zcodego.app.ui.splash.SplashScreen
import com.zcodego.app.ui.theme.ZCodeGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ingestIntent(intent)
        val app = application as ZCodeGoApp
        val controller = app.controller
        setContent {
            val settings by controller.settings.collectAsStateWithLifecycle()
            val ready by controller.sessionsReady.collectAsStateWithLifecycle()
            ZCodeGoTheme(themeMode = settings?.themeMode ?: ThemeMode.SYSTEM) {
                val current = settings
                if (current == null || !ready) {
                    SplashScreen()
                } else {
                    val boot = remember {
                        val pending = controller.linkInbox.peekAccepted()
                        val reopen = if (pending == null) {
                            SessionRestore.reopenId(
                                hostMode = current.hostMode,
                                reopenEnabled = current.reopenLastSession,
                                lastSessionId = current.lastSessionId,
                                sessions = controller.sessions.value,
                            )
                        } else {
                            null
                        }
                        current.hostMode to reopen
                    }
                    AppNav(
                        controller = controller,
                        initialHostMode = boot.first,
                        reopenSessionId = boot.second,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as ZCodeGoApp).processInForeground.value = true
    }

    override fun onStop() {
        (application as ZCodeGoApp).processInForeground.value = false
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ingestIntent(intent)
    }

    private fun ingestIntent(intent: Intent?) {
        val raw = RemoteIngest.rawText(
            action = intent?.action,
            dataString = intent?.dataString,
            shareText = intent?.getStringExtra(Intent.EXTRA_TEXT),
        ) ?: return
        val inbox = (application as ZCodeGoApp).controller.linkInbox
        when (val parsed = RemoteUrlPolicy.parseRemoteLink(raw)) {
            is ParseResult.Ok -> inbox.offerAccepted(parsed.link.url)
            is ParseResult.Reject -> inbox.offerRejected(parsed.reason)
        }
    }
}
