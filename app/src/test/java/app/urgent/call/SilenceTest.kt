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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceTest {
    private val ringing =
        SoundSettings(
            ringMuted = false,
            ringVolume = 5,
            audioMode = MODE_RINGTONE,
            interruptionFilter = INTERRUPTION_FILTER_ALL,
            priorityCategories = PRIORITY_CATEGORY_ALARMS,
            priorityCallSenders = PRIORITY_SENDERS_STARRED,
        )
    private val silent = ringing.copy(ringMuted = true)
    private val repeatCallersAllowed = priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_REPEAT_CALLERS)

    private fun ringsThrough(
        sound: SoundSettings,
        earlierCallAgeMillis: Long? = 3 * 60_000L,
        contact: CallerContact = CallerContact.NONE,
    ) = shouldRingThrough(sound, earlierCallAgeMillis, contact)

    private fun priority(
        categories: Int,
        senders: Int = PRIORITY_SENDERS_STARRED,
    ) = ringing.copy(
        interruptionFilter = INTERRUPTION_FILTER_PRIORITY,
        priorityCategories = categories,
        priorityCallSenders = senders,
    )

    @Test fun `a phone that rings on its own is left alone`() = assertFalse(ringsThrough(ringing))

    @Test fun `silent or vibrate mode rings through`() = assertTrue(ringsThrough(silent))

    @Test fun `a ring volume of zero rings through`() = assertTrue(ringsThrough(ringing.copy(ringVolume = 0)))

    @Test fun `a waiting call during a phone call stays quiet`() =
        assertFalse(ringsThrough(silent.copy(audioMode = MODE_IN_CALL)))

    @Test fun `a waiting call during a VoIP call stays quiet`() =
        assertFalse(ringsThrough(silent.copy(audioMode = MODE_IN_COMMUNICATION)))

    @Test fun `total silence mutes alarms too, so nothing would be heard`() =
        assertFalse(ringsThrough(silent.copy(interruptionFilter = INTERRUPTION_FILTER_NONE)))

    @Test fun `alarms-only mode mutes the call but not the alarm stream`() =
        assertTrue(ringsThrough(ringing.copy(interruptionFilter = INTERRUPTION_FILTER_ALARMS)))

    @Test fun `priority mode without calls rings through`() =
        assertTrue(
            ringsThrough(priority(PRIORITY_CATEGORY_ALARMS)),
        )

    @Test fun `priority mode with alarms off would not be heard`() = assertFalse(ringsThrough(priority(0)))

    @Test fun `priority mode with alarms off would not be heard on silent either`() =
        assertFalse(ringsThrough(priority(0).copy(ringMuted = true)))

    @Test fun `priority mode letting repeat callers through rings on its own`() =
        assertFalse(ringsThrough(repeatCallersAllowed))

    @Test fun `DND's repeat-caller threshold edge still rings on its own`() =
        assertFalse(ringsThrough(repeatCallersAllowed, DND_REPEAT_CALLERS_MILLIS))

    @Test fun `an earlier call past DND's threshold is not a repeat caller to DND`() =
        assertTrue(ringsThrough(repeatCallersAllowed, DND_REPEAT_CALLERS_MILLIS + 1))

    @Test fun `a first call rings through silent mode`() = assertTrue(ringsThrough(silent, earlierCallAgeMillis = null))

    @Test fun `a first call is not a repeat caller to DND`() =
        assertTrue(ringsThrough(repeatCallersAllowed, earlierCallAgeMillis = null))

    @Test fun `a first call is left alone when DND lets every caller through`() =
        assertFalse(
            ringsThrough(
                priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_ANY),
                earlierCallAgeMillis = null,
            ),
        )

    @Test fun `a 30-minute window catches a call DND's 15 minutes does not`() =
        assertTrue(ringsThrough(repeatCallersAllowed, 20 * 60_000L))

    @Test fun `priority mode letting every caller through rings on its own`() =
        assertFalse(
            ringsThrough(priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_ANY)),
        )

    @Test fun `priority mode letting only contacts through rings through`() =
        assertTrue(
            ringsThrough(priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_CONTACTS)),
        )

    @Test fun `a saved contact stays with the phone when only contacts get through`() =
        assertFalse(
            ringsThrough(
                priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_CONTACTS),
                contact = CallerContact.SAVED,
            ),
        )

    @Test fun `a starred contact stays with the phone when only starred contacts get through`() =
        assertFalse(
            ringsThrough(
                priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_STARRED),
                contact = CallerContact.STARRED,
            ),
        )

    @Test fun `a saved contact that is not starred rings through when only starred contacts get through`() =
        assertTrue(
            ringsThrough(
                priority(PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_STARRED),
                contact = CallerContact.SAVED,
            ),
        )

    @Test fun `a saved contact rings through when calls are not let through at all`() =
        assertTrue(ringsThrough(priority(PRIORITY_CATEGORY_ALARMS), contact = CallerContact.SAVED))

    @Test fun `silent mode wins over a priority policy that would let the call through`() =
        assertTrue(
            ringsThrough(
                priority(
                    PRIORITY_CATEGORY_ALARMS or PRIORITY_CATEGORY_REPEAT_CALLERS,
                ).copy(ringMuted = true),
            ),
        )
}
