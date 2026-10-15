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
 * The number of the one contact the user tapped. It is read through the grant the picker returns, so
 * the app asks for no contacts permission and never sees the rest of the list.
 */
fun ContentResolver.pickedNumber(pickedUri: Uri): String? =
    try {
        query(pickedUri, arrayOf(Phone.NORMALIZED_NUMBER, Phone.NUMBER), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.callerNumber() else null
        }
    } catch (e: RuntimeException) {
        // The URI comes from another app's picker, so any provider error means no number, not a crash.
        null
    }

private fun Cursor.callerNumber(): String? =
    stringAt(Phone.NORMALIZED_NUMBER)?.takeIf { it.isNotBlank() } ?: stringAt(Phone.NUMBER)

private fun Cursor.stringAt(columnName: String): String? {
    val column = getColumnIndex(columnName)
    return if (column >= 0) getString(column) else null
}
