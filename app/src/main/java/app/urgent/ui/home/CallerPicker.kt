package app.urgent.ui.home

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.DisplayNameSources
import android.telephony.PhoneNumberUtils

// The platform matches on trailing digits; a shorter number matches only in full, so it silences other callers.
const val MIN_CALLER_DIGITS = 7

fun storedCallerNumber(raw: String?): String? {
    val network = raw?.let(PhoneNumberUtils::extractNetworkPortion) ?: return null
    return network.takeIf(::isCallableNumber)
}

/** Operator codes such as *111*302*4# are saved in contacts, but no call ever comes from one. */
fun isCallableNumber(network: String): Boolean =
    network.count(Char::isDigit) >= MIN_CALLER_DIGITS && network.none { char -> char == '*' || char == '#' }

/** One row of the contacts list: a number of a contact, with the name the contact has for itself. */
data class PickedCaller(
    val name: String?,
    val forms: List<String>,
)

/**
 * Every phone number in the contacts list that Urgent can match on, one row per number, sorted by name.
 * Reading it needs READ_CONTACTS, which the Add from contacts button asks for when it is pressed.
 */
fun ContentResolver.readContactCallers(): List<PickedCaller> =
    try {
        // The provider returns one row per raw contact, so a contact linked from two accounts repeats.
        // Sorted by the provider's locale-aware key: COLLATE NOCASE folds only ASCII, so Ł would sort after Z.
        query(Phone.CONTENT_URI, contactColumns, null, null, Phone.SORT_KEY_PRIMARY)
            ?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val forms =
                            listOf(cursor.stringAt(Phone.NORMALIZED_NUMBER), cursor.stringAt(Phone.NUMBER))
                                .mapNotNull(::storedCallerNumber)
                        pickedCallerOf(
                            cursor.stringAt(Phone.DISPLAY_NAME),
                            cursor.intAt(Phone.DISPLAY_NAME_SOURCE),
                            forms,
                        )?.let { caller -> add(caller) }
                    }
                }
            }.orEmpty()
            .distinct()
    } catch (e: RuntimeException) {
        // A provider failure leaves the list empty rather than crashing the screen.
        emptyList()
    }

/** The row for one number, or null when the number has no form Urgent can match on. */
fun pickedCallerOf(
    displayName: String?,
    displayNameSource: Int?,
    forms: List<String>,
): PickedCaller? {
    if (forms.isEmpty()) return null
    return PickedCaller(name = callerName(displayName, displayNameSource), forms = forms.distinct())
}

/**
 * Whether a row matches what the user typed: part of its name, ignoring case, or the typed digits inside one of
 * its numbers. A blank search matches every row.
 */
fun PickedCaller.matches(query: String): Boolean {
    val text = query.trim()
    if (text.isEmpty()) return true
    val digits = text.filter(Char::isDigit)
    return name?.contains(text, ignoreCase = true) == true ||
        (digits.isNotEmpty() && forms.any { form -> form.filter(Char::isDigit).contains(digits) })
}

/**
 * The name the contact has for itself. When a contact has no name, the provider's display name falls back to
 * a nickname, an organisation or a number, and none of those is kept.
 */
fun callerName(
    displayName: String?,
    displayNameSource: Int?,
): String? {
    if (displayNameSource != DisplayNameSources.STRUCTURED_NAME) return null
    return displayName?.replace(Regex("\\s+"), " ")?.trim()?.takeIf(String::isNotEmpty)
}

private val contactColumns =
    arrayOf(Phone.NORMALIZED_NUMBER, Phone.NUMBER, Phone.DISPLAY_NAME, Phone.DISPLAY_NAME_SOURCE)

private fun Cursor.stringAt(columnName: String): String? {
    val column = getColumnIndex(columnName)
    return if (column >= 0) getString(column) else null
}

private fun Cursor.intAt(columnName: String): Int? {
    val column = getColumnIndex(columnName)
    return if (column >= 0 && !isNull(column)) getInt(column) else null
}
