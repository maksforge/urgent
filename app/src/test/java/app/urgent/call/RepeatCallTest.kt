package app.urgent.call

import android.provider.CallLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatCallTest {
    private val now = 1_000_000_000L
    private val minute = 60_000L
    private val exact: (String, String) -> Boolean = { a, b -> a == b }

    private fun repeat(
        vararg earlier: LoggedCall,
        window: Int = 15,
        incoming: String = "600100200",
        countDeclined: Boolean = true,
    ) = repeatCallAgeMillis(incoming, earlier.toList(), now, window, countDeclined, exact) != null

    private fun call(
        minutesAgo: Long,
        type: Int = CallLog.Calls.MISSED_TYPE,
        number: String = "600100200",
    ) = LoggedCall(number, now - minutesAgo * minute, type)

    @Test fun `a missed call from the same number inside the window is a repeat`() = assertTrue(repeat(call(3)))

    @Test fun `the window edge still counts`() = assertTrue(repeat(call(15)))

    @Test fun `a call older than the window is not a repeat`() = assertFalse(repeat(call(16)))

    @Test fun `the window is the chosen one`() = assertFalse(repeat(call(6), window = 5))

    @Test fun `a different number is not a repeat`() = assertFalse(repeat(call(3, number = "700100200")))

    @Test fun `an answered call counts`() = assertTrue(repeat(call(3, CallLog.Calls.INCOMING_TYPE)))

    @Test fun `a declined call counts`() = assertTrue(repeat(call(3, CallLog.Calls.REJECTED_TYPE)))

    @Test fun `a declined call does not count when declined calls are switched off`() =
        assertFalse(repeat(call(3, CallLog.Calls.REJECTED_TYPE), countDeclined = false))

    @Test fun `a missed call still counts when declined calls are switched off`() =
        assertTrue(repeat(call(3), countDeclined = false))

    @Test fun `a newer declined call outweighs an earlier missed one when declined calls are off`() =
        assertFalse(repeat(call(2, CallLog.Calls.REJECTED_TYPE), call(9), countDeclined = false))

    @Test fun `a newer missed call counts behind an earlier declined one`() =
        assertTrue(repeat(call(2), call(9, CallLog.Calls.REJECTED_TYPE), countDeclined = false))

    @Test fun `my own outgoing call does not count`() = assertFalse(repeat(call(3, CallLog.Calls.OUTGOING_TYPE)))

    @Test fun `a blocked number does not count`() = assertFalse(repeat(call(3, CallLog.Calls.BLOCKED_TYPE)))

    @Test fun `a row dated in the future does not count`() = assertFalse(repeat(call(-1)))

    @Test fun `no earlier calls is not a repeat`() = assertFalse(repeat())

    @Test fun `number matching is delegated`() =
        assertTrue(
            repeatCallAgeMillis("+48600100200", listOf(call(3)), now, 15, true) { a, b ->
                a.takeLast(9) == b.takeLast(9)
            } !=
                null,
        )

    @Test fun `the age is the newest matching call's`() =
        assertEquals(
            4 * minute,
            repeatCallAgeMillis("600100200", listOf(call(12), call(4), call(20)), now, 30, true, exact),
        )

    @Test fun `no repeat has no age`() =
        assertNull(
            repeatCallAgeMillis("600100200", listOf(call(31)), now, 30, true, exact),
        )
}
