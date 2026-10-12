package app.urgent.call

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.NotificationManager.Policy
import android.media.AudioManager

/** The phone's sound settings at the moment a call rings, as plain values a JVM test can build. */
data class SoundSettings(
    val ringerMode: Int,
    val ringVolume: Int,
    val audioMode: Int,
    val interruptionFilter: Int,
    val priorityCategories: Int,
    val priorityCallSenders: Int,
)

/**
 * Whether Urgent should ring for a repeated call: the phone would let it go by without a sound, and
 * a ringtone on the alarm stream would actually be heard.
 *
 * Do Not Disturb is judged from its policy rather than `matchesCallFilter`, which needs
 * `READ_CONTACTS` this app does not ask for. A policy that lets repeat callers through rings on its
 * own, but only for an earlier call inside its own threshold, which can be shorter than the user's
 * window. One that lets calls through only from contacts cannot be judged without reading them, so
 * Urgent rings then.
 *
 * The alarms bit is new in Android 9; before it, `soundSettings()` sets the bit itself.
 */
@SuppressLint("InlinedApi")
fun shouldRingThrough(
    sound: SoundSettings,
    earlierCallAgeMillis: Long,
): Boolean {
    val onACall =
        sound.audioMode == AudioManager.MODE_IN_CALL || sound.audioMode == AudioManager.MODE_IN_COMMUNICATION
    if (onACall) return false

    val priorityOnly = sound.interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    val alarmsMuted =
        sound.interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            (priorityOnly && (sound.priorityCategories and Policy.PRIORITY_CATEGORY_ALARMS) == 0)
    if (alarmsMuted) return false

    if (sound.ringerMode != AudioManager.RINGER_MODE_NORMAL || sound.ringVolume == 0) return true

    return when (sound.interruptionFilter) {
        NotificationManager.INTERRUPTION_FILTER_ALARMS -> true
        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> !dndRingsOnItsOwn(sound, earlierCallAgeMillis)
        else -> false
    }
}

/**
 * AOSP's `config_zen_repeat_callers_threshold`, inclusive. It is a private framework resource, so a
 * ROM that changes it moves the edge without telling us.
 */
const val DND_REPEAT_CALLERS_MILLIS = 15 * 60_000L

private fun dndRingsOnItsOwn(
    sound: SoundSettings,
    earlierCallAgeMillis: Long,
): Boolean {
    val categories = sound.priorityCategories
    val repeatCallersAllowed = (categories and Policy.PRIORITY_CATEGORY_REPEAT_CALLERS) != 0
    if (repeatCallersAllowed && earlierCallAgeMillis <= DND_REPEAT_CALLERS_MILLIS) return true
    return (categories and Policy.PRIORITY_CATEGORY_CALLS) != 0 &&
        sound.priorityCallSenders == Policy.PRIORITY_SENDERS_ANY
}
