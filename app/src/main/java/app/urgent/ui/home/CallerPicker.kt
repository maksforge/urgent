package app.urgent.ui.home

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone

fun pickCallerIntent(): Intent = Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)

/**
 * The number of the one contact the user tapped. It is read through the grant the picker returns, so
 * the app asks for no contacts permission and never sees the rest of the list.
 */
fun ContentResolver.pickedNumber(pickedUri: Uri): String? =
    try {
        query(pickedUri, arrayOf(Phone.NUMBER), null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(Phone.NUMBER)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        }
    } catch (e: RuntimeException) {
        // The URI comes from another app's picker, so any provider error means no number, not a crash.
        null
    }
