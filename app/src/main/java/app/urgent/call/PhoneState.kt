package app.urgent.call

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils

/**
 * Both are runtime permissions, and both are needed: `READ_PHONE_STATE` delivers the ringing
 * broadcast at all, and since Android 10 the caller's number arrives only to an app that also holds
 * `READ_CALL_LOG`. That one also lets us read the log to find the earlier call.
 */
val CALL_PERMISSIONS = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)

fun Context.hasCallPermissions(): Boolean =
    CALL_PERMISSIONS.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

/** Calls logged since [sinceMillis], or nothing if the log cannot be read. */
fun Context.callsSince(sinceMillis: Long): List<LoggedCall> {
    val columns = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE)
    val cursor =
        try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                columns,
                "${CallLog.Calls.DATE} >= ?",
                arrayOf(sinceMillis.toString()),
                null,
            )
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()

    return cursor.use {
        buildList {
            while (it.moveToNext()) {
                val number = it.getString(0) ?: continue
                add(LoggedCall(number = number, atMillis = it.getLong(1), type = it.getInt(2)))
            }
        }
    }
}

fun Context.isRepeatCallNow(
    incoming: String,
    windowMinutes: Int,
): Boolean {
    val now = System.currentTimeMillis()
    return isRepeatCall(
        incoming = incoming,
        earlier = callsSince(now - windowMinutes * 60_000L),
        nowMillis = now,
        windowMinutes = windowMinutes,
        sameNumber = PhoneNumberUtils::compare,
    )
}

/**
 * Whether the phone would let this call go by without a sound — the only case Urgent steps in.
 *
 * Silent or vibrate mode, or a ring volume turned to zero, mute the ringtone outright. Do Not Disturb
 * is subtler: its own policy may already let the call through (starred contacts, its own repeat
 * callers), and ringing on top of the phone's ringtone would be two sounds at once. Android 13 can
 * answer that question for us; before it, Do Not Disturb counts as silent.
 */
fun Context.wouldStaySilent(number: String): Boolean {
    val audio = getSystemService(AudioManager::class.java) ?: return false
    if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return true
    if (audio.getStreamVolume(AudioManager.STREAM_RING) == 0) return true

    val notifications = getSystemService(NotificationManager::class.java) ?: return false
    val doNotDisturb = notifications.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
    if (!doNotDisturb) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

    val systemLetsItThrough =
        try {
            notifications.matchesCallFilter(Uri.fromParts("tel", number, null))
        } catch (e: SecurityException) {
            false
        }
    return !systemLetsItThrough
}

/** Whether a call is still ringing or connected — `isRinging` itself is not public API. */
fun Context.isCallStillThere(): Boolean =
    try {
        getSystemService(TelecomManager::class.java)?.isInCall == true
    } catch (e: SecurityException) {
        false
    }
