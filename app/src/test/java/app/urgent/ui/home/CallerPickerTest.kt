package app.urgent.ui.home

import android.provider.ContactsContract.DisplayNameSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerPickerTest {
    @Test fun `a structured name is kept, on one line`() =
        assertEquals("Anna Kowalska", callerName(" Anna\n  Kowalska ", DisplayNameSources.STRUCTURED_NAME))

    @Test fun `a nickname is not a name`() = assertNull(callerName("Kicia", DisplayNameSources.NICKNAME))

    @Test fun `an organisation or a number standing in for a name is not a name`() {
        assertNull(callerName("Acme", DisplayNameSources.ORGANIZATION))
        assertNull(callerName("+48600100200", DisplayNameSources.PHONE))
    }

    @Test fun `no name, or a blank one, is no name`() {
        assertNull(callerName(null, DisplayNameSources.STRUCTURED_NAME))
        assertNull(callerName("   ", DisplayNameSources.STRUCTURED_NAME))
    }

    @Test fun `a number with no usable form gives no row`() =
        assertNull(pickedCallerOf("Anna", DisplayNameSources.STRUCTURED_NAME, emptyList()))

    @Test fun `a row keeps the structured name and its forms, each once`() =
        assertEquals(
            PickedCaller("Anna Kowalska", listOf("+48600100200", "600100200")),
            pickedCallerOf(
                " Anna  Kowalska ",
                DisplayNameSources.STRUCTURED_NAME,
                listOf("+48600100200", "600100200", "+48600100200"),
            ),
        )

    @Test fun `a search matches part of a name, ignoring case`() =
        assertTrue(PickedCaller("Anna Kowalska", listOf("600100200")).matches("KOWAL"))

    @Test fun `a search matches typed digits inside any number`() =
        assertTrue(PickedCaller("Anna", listOf("+48600100200", "600100200")).matches("600 100"))

    @Test fun `a blank search matches every row and a miss matches none`() {
        assertTrue(PickedCaller(null, listOf("600100200")).matches("  "))
        assertFalse(PickedCaller("Anna", listOf("600100200")).matches("zzz"))
    }

    @Test fun `operator codes with a star or a hash are not callers`() {
        assertFalse(isCallableNumber("*111*302*4#"))
        assertFalse(isCallableNumber("#100#"))
    }

    @Test fun `a number with enough digits and no code is a caller`() {
        assertTrue(isCallableNumber("+48600100200"))
        assertFalse(isCallableNumber("123456"))
    }
}
