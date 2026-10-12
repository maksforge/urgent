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
 * Whether the call ringing now is the second one from [incoming] inside the window.
 *
 * The call that is ringing is not in [earlier] yet — Android writes a call to the log when it ends —
 * so any matching row is a genuinely earlier call. [sameNumber] is passed in because number matching
 * is the platform's job (`+48 600…` and `600…` are the same phone), and the platform's matcher is not
 * callable from a JVM test.
 */
fun isRepeatCall(
    incoming: String,
    earlier: List<LoggedCall>,
    nowMillis: Long,
    windowMinutes: Int,
    sameNumber: (String, String) -> Boolean,
): Boolean {
    val windowMillis = windowMinutes * 60_000L
    return earlier.any { call ->
        call.type in COUNTED_TYPES &&
            nowMillis - call.atMillis in 0..windowMillis &&
            sameNumber(call.number, incoming)
    }
}
