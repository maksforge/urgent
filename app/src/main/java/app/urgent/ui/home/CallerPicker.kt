package app.urgent.ui.home

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils

fun pickCallerIntent(): Intent = Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)

// The platform matches on trailing digits; a shorter number matches only in full, so it silences other callers.
const val MIN_CALLER_DIGITS = 7

fun storedCallerNumber(raw: String?): String? {
    val network = raw?.let(PhoneNumberUtils::extractNetworkPortion) ?: return null
    return network.takeIf { number -> number.count(Char::isDigit) >= MIN_CALLER_DIGITS }
}

/**
 * The numbers of the one contact the user tapped: the normalised form first, then the number as written.
 * The normalised form takes its country from the phone at save time, so it can differ from the number that
 * calls, which is why both are kept. It is read through the grant the picker returns, so the app asks for
 * no contacts permission and never sees the rest of the list.
 */
fun ContentResolver.pickedNumbers(pickedUri: Uri): List<String> =
    try {
        query(pickedUri, arrayOf(Phone.NORMALIZED_NUMBER, Phone.NUMBER), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.callerNumbers() else emptyList()
            }.orEmpty()
    } catch (e: RuntimeException) {
        // The URI comes from another app's picker, so any provider error means no number, not a crash.
        emptyList()
    }

private fun Cursor.callerNumbers(): List<String> =
    listOf(Phone.NORMALIZED_NUMBER, Phone.NUMBER).mapNotNull { stringAt(it) }.filter { it.isNotBlank() }

private fun Cursor.stringAt(columnName: String): String? {
    val column = getColumnIndex(columnName)
    return if (column >= 0) getString(column) else null
}
