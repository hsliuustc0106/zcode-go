package com.zcodego.app.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zcodego.app.MainActivity
import com.zcodego.app.R

/**
 * Posts a notification when the official page title changes while this app is backgrounded.
 * The title is sanitized and URLs are dropped. This is not an agent-status API.
 */
object PageAttentionNotifier {
    private const val CHANNEL_ID = "remote_attention"
    private const val NOTIFICATION_ID = 42
    private var lastTitle: String? = null

    fun onTitle(context: Context, rawTitle: String, appInForeground: Boolean, sessionId: String?) {
        val sanitized = PageAttention.sanitize(rawTitle)
        val notable = PageAttention.notableTitle(lastTitle, rawTitle, appInForeground)
        if (sanitized != null) lastTitle = sanitized
        if (notable != null) show(context.applicationContext, notable, sessionId)
    }

    private fun show(context: Context, title: String, sessionId: String?) {
        val text = AttentionOpen.notificationText(title) ?: return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.attention_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                AttentionOpen.sessionId(sessionId)?.let { putExtra(AttentionOpen.EXTRA_SESSION_ID, it) }
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_z)
            .setContentTitle(context.getString(R.string.attention_title))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }
}
