package app.urgent.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.urgent.R
import app.urgent.theme.DarkColors
import app.urgent.theme.LightColors

/**
 * Rings and vibrates for a repeated call while the phone itself stays silent.
 *
 * **The sound goes out as an alarm, not as a ringtone.** Silent and vibrate mode mute the ring
 * stream at the platform level, and nothing short of changing the user's ringer mode would unmute it
 * — which needs Do Not Disturb access and leaves the phone loud if anything goes wrong before it is
 * put back. The alarm stream is outside ringer mode altogether, so the phone's settings are never
 * touched, and the volume is the alarm volume the user already chose.
 *
 * A foreground service because a call rings for up to a minute and a broadcast receiver has seconds.
 * Starting one from the background is refused since Android 12 unless the app is exempt from battery
 * optimisation — which is why the home screen asks for that exemption.
 */
class RingThroughService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val giveUp = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_SILENCE) {
            stopSelf()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            ringingNotification(),
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
        if (player == null) startRinging()

        // A call that is never answered still stops ringing on the phone's side; this is the bound
        // for the case where the broadcast saying so never reaches us.
        handler.removeCallbacks(giveUp)
        handler.postDelayed(giveUp, MAX_RING_MILLIS)

        // NOT_STICKY: if Android kills this mid-ring, the call it was for is long over by the time a
        // restart would happen.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(giveUp)
        player?.run {
            stop()
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        super.onDestroy()
    }

    private fun startRinging() {
        player = ringtonePlayer()
        vibrator = vibrate()
    }

    /**
     * The user's own ringtone where it can be read, falling back to the phone's default and then to
     * the default alarm. A ringtone that is a file on shared storage cannot be opened without a
     * storage permission we do not ask for, so the fallbacks are reached in practice.
     */
    private fun ringtonePlayer(): MediaPlayer? {
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        val candidates =
            listOfNotNull(
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE),
                Settings.System.DEFAULT_RINGTONE_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            )
        for (uri in candidates) {
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(attributes)
                player.setDataSource(this, uri)
                player.isLooping = true
                player.prepare()
                player.start()
                return player
            } catch (e: Exception) {
                Log.w(TAG, "Could not play $uri", e)
                player.release()
            }
        }
        return null
    }

    private fun vibrate(): Vibrator? {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            } ?: return null
        val pattern = VibrationEffect.createWaveform(longArrayOf(0, 1_000, 1_000), 0)
        // Marked as an alarm for the same reason as the sound: vibrate-mode rules and silent mode
        // suppress ringtone vibration, not alarm vibration.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(pattern, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                pattern,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
        return vibrator
    }

    private fun ringingNotification(): Notification {
        val notifications = getSystemService(NotificationManager::class.java)
        // A channel's importance is fixed once it exists on a phone, so raising it takes a new id.
        notifications.deleteNotificationChannel(RETIRED_CHANNEL_ID)
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_ringing_name),
                // High so it pops up with Silence in reach, silent because this service is the sound.
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.channel_ringing_description)
                setSound(null, null)
                enableVibration(false)
            },
        )
        val silence =
            PendingIntent.getService(
                this,
                0,
                Intent(this, RingThroughService::class.java).setAction(ACTION_SILENCE),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ringingTint())
            .setContentTitle(getString(R.string.ringing_title))
            .setContentText(getString(R.string.ringing_body))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .addAction(0, getString(R.string.ringing_silence), silence)
            .build()
    }

    /** Amber, the colour of ringing, from the scheme the shade is drawn in; the system adjusts it for contrast. */
    private fun ringingTint(): Int {
        val night =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return (if (night) DarkColors else LightColors).tertiary.toArgb()
    }

    companion object {
        private const val TAG = "RingThrough"
        private const val CHANNEL_ID = "ringing_heads_up"
        private const val RETIRED_CHANNEL_ID = "ringing"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_SILENCE = "app.urgent.action.SILENCE"
        private const val MAX_RING_MILLIS = 60_000L

        /**
         * Starting a foreground service from the background throws on Android 12+ unless the app is
         * exempt; the home screen says so, and here the refusal is logged rather than crashing the
         * receiver that asked.
         */
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, RingThroughService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Not allowed to start ringing from the background", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RingThroughService::class.java))
        }
    }
}
