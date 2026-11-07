package com.zcodego.app.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zcodego.app.MainActivity
import com.zcodego.app.R

/**
 * Visible foreground notification while a remote page is on screen.
 * It does not run a local agent. Leaving the page stops the service.
 */
class RemoteKeepAliveService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = KeepAliveLabel.safe(intent?.getStringExtra(EXTRA_LABEL))
            ?: getString(R.string.remote_title)
        val sessionId = AttentionOpen.sessionId(intent?.getStringExtra(AttentionOpen.EXTRA_SESSION_ID))
        val notification = buildNotification(label, sessionId)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(label: String, sessionId: String?): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.keepalive_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager?.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                sessionId?.let { putExtra(AttentionOpen.EXTRA_SESSION_ID, it) }
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_z)
            .setContentTitle(getString(R.string.keepalive_title))
            .setContentText(getString(R.string.keepalive_body, label))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "remote_keepalive"
        private const val NOTIFICATION_ID = 41
        private const val EXTRA_LABEL = "label"

        fun start(context: Context, label: String, sessionId: String?) {
            val safe = KeepAliveLabel.safe(label).orEmpty()
            val intent = Intent(context, RemoteKeepAliveService::class.java).putExtra(EXTRA_LABEL, safe)
            AttentionOpen.sessionId(sessionId)?.let { intent.putExtra(AttentionOpen.EXTRA_SESSION_ID, it) }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, RemoteKeepAliveService::class.java)) }
        }
    }
}
