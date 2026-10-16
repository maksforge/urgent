package app.urgent.call

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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.urgent.MainActivity
import app.urgent.R

/**
 * Keeps the process running while the user has switched on *Run in the background*, so the
 * phone-state receiver never waits for Android or the ROM to start Urgent again.
 *
 * A foreground service is the only way an app stays up on purpose, and it needs the notification:
 * that is also how the user sees Urgent is on. Being a service rather than the activity is what lets
 * swiping Urgent out of Recents leave it running; a ROM may still stop it, which is what the Xiaomi
 * row on the home screen is for.
 */
class KeepAliveService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        ServiceCompat.startForeground(this, NOTIFICATION_ID, keepAliveNotification(), serviceType())
        // START_STICKY: if Android stops this for memory, it comes back. A ring must not, because it ends with its call.
        return START_STICKY
    }

    private fun serviceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private fun keepAliveNotification(): Notification {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_keep_alive_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.channel_keep_alive_description) },
        )
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.keep_alive_title))
            .setContentText(getString(R.string.keep_alive_body))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "KeepAlive"
        private const val CHANNEL_ID = "keep_alive"
        private const val NOTIFICATION_ID = 2

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, KeepAliveService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Not allowed to keep running from the background", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}
