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
 * to mean "not now, even if they try again": then declining a caller's latest call means their next
 * call is not a repeat, and a later missed or answered call starts counting again.
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
    val newest =
        earlier
            .filter { call -> call.type in COUNTED_TYPES && sameNumber(call.number, incoming) }
            .filter { call -> nowMillis - call.atMillis in 0..windowMillis }
            .maxByOrNull { call -> call.atMillis }
            ?: return null
    if (!countDeclined && newest.type == CallLog.Calls.REJECTED_TYPE) return null
    return nowMillis - newest.atMillis
}

/** Whether [incoming] is one of a chosen caller's numbers. With none chosen, anyone is. */
fun isChosenCaller(
    incoming: String,
    chosen: Set<String>,
    sameNumber: (String, String) -> Boolean,
): Boolean =
    chosen.isEmpty() || chosen.any { entry -> ChosenCaller.fromEntry(entry).numbers.any { sameNumber(it, incoming) } }
