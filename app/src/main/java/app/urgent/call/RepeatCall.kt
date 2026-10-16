package app.urgent.call

import android.provider.CallLog
import app.urgent.data.CallerRules
import app.urgent.data.ContactScope

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

fun isChosenCaller(
    incoming: String,
    chosen: Set<String>,
    sameNumber: (String, String) -> Boolean,
): Boolean = chosen.any { entry -> ChosenCaller.fromEntry(entry).numbers.any { sameNumber(it, incoming) } }

/**
 * Whether a repeated call from [incoming] may ring. Nothing restricts anyone while the chosen list is
 * off or empty and the contacts setting is off. Otherwise a chosen caller may, and so may a contact
 * that [rules] admits: any saved one, or only a starred one.
 */
fun isAllowedCaller(
    incoming: String,
    rules: CallerRules,
    sameNumber: (String, String) -> Boolean,
    contact: CallerContact,
): Boolean {
    val listRestricts = rules.useChosen && rules.chosen.isNotEmpty()
    if (!listRestricts && rules.contactScope == ContactScope.OFF) return true
    if (rules.useChosen && isChosenCaller(incoming, rules.chosen, sameNumber)) return true
    return when (rules.contactScope) {
        ContactScope.OFF -> false
        ContactScope.EVERYONE -> contact != CallerContact.NONE
        ContactScope.STARRED -> contact == CallerContact.STARRED
    }
}
