package app.urgent.call

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.NotificationManager.Policy
import android.media.AudioManager

/** The phone's sound settings at the moment a call rings, as plain values a JVM test can build. */
data class SoundSettings(
    val ringerMode: Int,
    val ringMuted: Boolean,
    val ringVolume: Int,
    val audioMode: Int,
    val interruptionFilter: Int,
    val priorityCategories: Int,
    val priorityCallSenders: Int,
)

/**
 * Whether Urgent should ring for a call it would ring for: the phone would let it go by without a
 * sound, and a ringtone on the alarm stream would actually be heard. [earlierCallAgeMillis] is the
 * age [dndRepeatCallAgeMillis] reads, `null` when Do Not Disturb's repeat-callers rule sees no earlier call.
 *
 * Do Not Disturb is judged from its policy and the caller's standing in the contacts. A policy that
 * lets repeat callers through rings on its own, but only for an earlier call inside its own threshold,
 * which can be shorter than the user's window. A policy that lets only contacts through rings on its
 * own for a saved contact, and one that lets only starred contacts through does so for a starred
 * contact, so [contact] says whether the phone would already let this caller through.
 *
 * The alarms bit is new in Android 9; before it, `soundSettings()` sets the bit itself.
 */
@SuppressLint("InlinedApi")
fun shouldRingThrough(
    sound: SoundSettings,
    earlierCallAgeMillis: Long?,
    contact: CallerContact,
): Boolean {
    val onACall =
        sound.audioMode == AudioManager.MODE_IN_CALL || sound.audioMode == AudioManager.MODE_IN_COMMUNICATION
    if (onACall) return false

    val priorityOnly = sound.interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    val alarmsMuted =
        sound.interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            (priorityOnly && (sound.priorityCategories and Policy.PRIORITY_CATEGORY_ALARMS) == 0)
    if (alarmsMuted) return false

    // Priority only reports the ringer as silent even while it lets calls ring, so `ringerMode` counts
    // only with Do Not Disturb off. A muted ring stream counts everywhere, but Priority only leaves the
    // stream unmuted while it lets any ringer sound through, messages or reminders included, so the
    // policy check below still decides there.
    val ringerOff =
        sound.interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL &&
            sound.ringerMode != AudioManager.RINGER_MODE_NORMAL
    if (ringerOff || sound.ringMuted || sound.ringVolume == 0) return true

    return when (sound.interruptionFilter) {
        NotificationManager.INTERRUPTION_FILTER_ALARMS -> true
        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> !dndRingsOnItsOwn(sound, earlierCallAgeMillis, contact)
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
    earlierCallAgeMillis: Long?,
    contact: CallerContact,
): Boolean {
    val categories = sound.priorityCategories
    val repeatCallersAllowed = (categories and Policy.PRIORITY_CATEGORY_REPEAT_CALLERS) != 0
    if (repeatCallersAllowed && earlierCallAgeMillis != null && earlierCallAgeMillis <= DND_REPEAT_CALLERS_MILLIS) {
        return true
    }
    if ((categories and Policy.PRIORITY_CATEGORY_CALLS) == 0) return false
    return when (sound.priorityCallSenders) {
        Policy.PRIORITY_SENDERS_ANY -> true
        Policy.PRIORITY_SENDERS_CONTACTS -> contact != CallerContact.NONE
        Policy.PRIORITY_SENDERS_STARRED -> contact == CallerContact.STARRED
        else -> false
    }
}
