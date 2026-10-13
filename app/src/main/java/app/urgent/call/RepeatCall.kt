package app.urgent.call

import android.provider.CallLog

/** One row of the phone's call log, reduced to what deciding a repeat needs. */
data class LoggedCall(
    val number: String,
    val atMillis: Long,
    val type: Int,
)

/**
 * The call-log types that count as "they already tried to reach you". Outgoing calls are you
 * reaching them, and a blocked number is one you have already said no to.
 */
private val COUNTED_TYPES =
    setOf(
        CallLog.Calls.INCOMING_TYPE,
        CallLog.Calls.MISSED_TYPE,
        CallLog.Calls.REJECTED_TYPE,
        CallLog.Calls.VOICEMAIL_TYPE,
    )

/**
 * How long ago [incoming] last called inside the window, or `null` when the call ringing now is not
 * a repeat. A declined call counts unless [countDeclined] is off, for the user who declines a caller
 * to mean "not now, even if they try again".
 *
 * The call that is ringing is not in [earlier] yet — Android writes a call to the log when it ends —
 * so any matching row is a genuinely earlier call. [sameNumber] is passed in because number matching
 * is the platform's job (`+48 600…` and `600…` are the same phone), and the platform's matcher is not
 * callable from a JVM test.
 */
fun repeatCallAgeMillis(
    incoming: String,
    earlier: List<LoggedCall>,
    nowMillis: Long,
    windowMinutes: Int,
    countDeclined: Boolean,
    sameNumber: (String, String) -> Boolean,
): Long? {
    val windowMillis = windowMinutes * 60_000L
    return earlier
        .filter { call -> call.type in COUNTED_TYPES && sameNumber(call.number, incoming) }
        .filter { call -> countDeclined || call.type != CallLog.Calls.REJECTED_TYPE }
        .map { call -> nowMillis - call.atMillis }
        .filter { age -> age in 0..windowMillis }
        .minOrNull()
}
