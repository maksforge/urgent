package app.urgent.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import app.urgent.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Hears every change of call state, and is the app's only way in while it is not open.
 *
 * Declared in the manifest rather than registered from code, so Android starts the process for it.
 * `PHONE_STATE` is one of the few broadcasts still delivered to a manifest receiver since Android 8;
 * on Xiaomi the ROM additionally refuses to start the process for it unless autostart is granted.
 */
class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_RINGING) {
            // Answered, declined or hung up: whatever we started ringing has no call left to announce.
            RingThroughService.stop(context)
            return
        }

        // Since Android 9 the broadcast arrives twice per call: once without the number, and once
        // with it for apps holding READ_CALL_LOG. Only the second one can be acted on.
        @Suppress("DEPRECATION")
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        if (number.isNullOrBlank()) return

        val app = context.applicationContext as MainApplication

        // `goAsync` keeps the broadcast open after `onReceive` returns, so the preference and
        // call-log reads can run off the main thread. Without it Android considers the receiver
        // finished — and the process killable — the moment this function returns. It is roughly a
        // promise the system waits on, and `finish()` is its resolve; it must be called on every path.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = app.preferences
                if (!preferences.ringThrough.first()) return@launch
                if (!app.isRepeatCallNow(number, preferences.repeatWindowMinutes.first())) return@launch
                val sound = app.soundSettings() ?: return@launch
                if (!shouldRingThrough(sound)) return@launch
                // The reads above take a moment; a caller who gave up meanwhile has already sent the
                // idle broadcast, and starting now would ring for a call that no longer exists.
                if (!app.isCallStillThere()) return@launch
                RingThroughService.start(app)
            } finally {
                pending.finish()
            }
        }
    }
}
