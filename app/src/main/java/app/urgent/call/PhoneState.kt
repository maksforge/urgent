package app.urgent.call

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils

/**
 * Both are runtime permissions, and both are needed: `READ_PHONE_STATE` delivers the ringing
 * broadcast at all, and since Android 9 the caller's number arrives only to an app that also holds
 * `READ_CALL_LOG`. That one also lets us read the log to find the earlier call.
 */
val CALL_PERMISSIONS = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)

fun Context.hasCallPermissions(): Boolean =
    CALL_PERMISSIONS.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

fun Context.hasContactsPermission(): Boolean =
    checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

/**
 * Whether a call from [number] comes from a saved contact. The platform matches the number itself, so
 * `+48 600…` and `600…` are the same phone.
 */
fun Context.isSavedContact(number: String): Boolean {
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    val cursor =
        try {
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
        } catch (e: SecurityException) {
            null
        }
    return cursor?.use { it.moveToFirst() } == true
}

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

fun Context.repeatCallAgeMillisNow(
    incoming: String,
    windowMinutes: Int,
    countDeclined: Boolean,
): Long? {
    val now = System.currentTimeMillis()
    return repeatCallAgeMillis(
        incoming = incoming,
        earlier = callsSince(now - windowMinutes * 60_000L),
        nowMillis = now,
        windowMinutes = windowMinutes,
        countDeclined = countDeclined,
        sameNumber = PhoneNumberUtils::compare,
    )
}

/** The live sound settings [shouldRingThrough] decides from; reading the DND policy needs no access grant. */
@SuppressLint("InlinedApi")
fun Context.soundSettings(): SoundSettings? {
    val audio = getSystemService(AudioManager::class.java) ?: return null
    val notifications = getSystemService(NotificationManager::class.java) ?: return null
    // From Android 15 the consolidated policy is the one in force, with schedules and modes applied;
    // before it, the user's manual policy is the closest the platform will tell an app.
    val policy =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            notifications.consolidatedNotificationPolicy
        } else {
            notifications.notificationPolicy
        }
    // Before Android 9 priority mode always let alarms through, and the policy had no bit for it.
    val alarmsAlwaysAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    val categories = policy?.priorityCategories ?: NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
    return SoundSettings(
        ringerMode = audio.ringerMode,
        ringVolume = audio.getStreamVolume(AudioManager.STREAM_RING),
        audioMode = audio.mode,
        interruptionFilter = notifications.currentInterruptionFilter,
        priorityCategories =
            if (alarmsAlwaysAllowed) categories or NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS else categories,
        priorityCallSenders = policy?.priorityCallSenders ?: NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
    )
}

/** Whether a call is still ringing or connected — `isRinging` itself is not public API. */
fun Context.isCallStillThere(): Boolean =
    try {
        getSystemService(TelecomManager::class.java)?.isInCall == true
    } catch (e: SecurityException) {
        false
    }
