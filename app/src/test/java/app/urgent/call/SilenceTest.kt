package app.urgent.call

import android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.NotificationManager.INTERRUPTION_FILTER_NONE
import android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
import android.app.NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
import android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
import android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
import android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY
import android.app.NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
import android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED
import android.media.AudioManager.MODE_IN_CALL
import android.media.AudioManager.MODE_IN_COMMUNICATION
import android.media.AudioManager.MODE_RINGTONE
import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceTest {
    private val ringing =
        SoundSettings(
            ringerMode = RINGER_MODE_NORMAL,
            ringVolume = 5,
            audioMode = MODE_RINGTONE,
            interruptionFilter = INTERRUPTION_FILTER_ALL,
            priorityCategories = PRIORITY_CATEGORY_ALARMS,
            priorityCallSenders = PRIORITY_SENDERS_STARRED,
        )
    private val silent = ringing.copy(ringerMode = RINGER_MODE_SILENT)

    private fun priority(
        categories: Int,
        senders: Int = PRIORITY_SENDERS_STARRED,
    ) = ringing.copy(
        interruptionFilter = INTERRUPTION_FILTER_PRIORITY,
        priorityCategories = categories,
        priorityCallSenders = senders,
    )

    @Test fun `a phone that rings on its own is left alone`() = assertFalse(shouldRingThrough(ringing))

    @Test fun `silent mode rings through`() = assertTrue(shouldRingThrough(silent))

    @Test fun `vibrate mode rings through`() =
        assertTrue(
            shouldRingThrough(ringing.copy(ringerMode = RINGER_MODE_VIBRATE)),
        )

    @Test fun `a ring volume of zero rings through`() = assertTrue(shouldRingThrough(ringing.copy(ringVolume = 0)))

    @Test fun `a waiting call during a phone call stays quiet`() =
        assertFalse(shouldRingThrough(silent.copy(audioMode = MODE_IN_CALL)))

    @Test fun `a waiting call during a VoIP call stays quiet`() =
        assertFalse(shouldRingThrough(silent.copy(audioMode = MODE_IN_COMMUNICATION)))

    @Test fun `total silence mutes alarms too, so nothing would be heard`() =
        assertFalse(shouldRingThrough(silent.copy(interruptionFilter = INTERRUPTION_FILTER_NONE)))

    @Test fun `alarms-only mode mutes the call but not the alarm stream`() =
        assertTrue(shouldRingThrough(ringing.copy(interruptionFilter = INTERRUPTION_FILTER_ALARMS)))

    @Test fun `priority mode without calls rings through`() =
        assertTrue(shouldRingThrough(priority(PRIORITY_CATEGORY_ALARMS)))

    @Test fun `priority mode with alarms off would not be heard`() = assertFalse(shouldRingThrough(priority(0)))

    @Test fun `priority mode with alarms off would not be heard on silent either`() =
        assertFalse(shouldRingThrough(priority(0).copy(ringerMode = RINGER_MODE_SILENT)))

    @Test fun `priority mode letting repeat callers through rings on its own`() =
        assertFalse(shouldRingThrough(priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_REPEAT_CALLERS)))

    @Test fun `priority mode letting every caller through rings on its own`() =
        assertFalse(
            shouldRingThrough(priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_ANY)),
        )

    @Test fun `priority mode letting only contacts through rings through`() =
        assertTrue(
            shouldRingThrough(priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_CONTACTS)),
        )

    @Test fun `silent mode wins over a priority policy that would let the call through`() =
        assertTrue(
            shouldRingThrough(
                priority(
                    PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_REPEAT_CALLERS,
                ).copy(ringerMode = RINGER_MODE_SILENT),
            ),
        )
}
