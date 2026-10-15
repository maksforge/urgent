package app.urgent.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChosenCallerTest {
    private val exact: (String, String) -> Boolean = { a, b -> a == b }
    private val suffix: (String, String) -> Boolean = { a, b -> a.takeLast(7) == b.takeLast(7) }

    private fun rings(
        incoming: String,
        chosen: Set<String> = emptySet(),
        contactsOn: Boolean = false,
        savedContact: Boolean = false,
        sameNumber: (String, String) -> Boolean = exact,
    ): Boolean = isAllowedCaller(incoming, chosen, contactsOn, sameNumber) { savedContact }

    @Test fun `anyone rings when no caller is chosen and contacts are off`() = assertTrue(rings("600100200"))

    @Test fun `a chosen caller rings`() = assertTrue(rings("600100200", setOf("600100200")))

    @Test fun `a caller who is not chosen does not ring`() = assertFalse(rings("700100200", setOf("600100200")))

    @Test fun `a caller with several numbers rings on any of them`() =
        assertTrue(rings("+48600100200", setOf("+49711111111 600100200"), sameNumber = suffix))

    @Test fun `a name after the numbers does not change who rings`() =
        assertTrue(rings("600100200", setOf("600100200\nAnna Kowalska")))

    @Test fun `only a saved contact rings when contacts are on and none are chosen`() {
        assertTrue(rings("600100200", contactsOn = true, savedContact = true))
        assertFalse(rings("700100200", contactsOn = true, savedContact = false))
    }

    @Test fun `a saved contact rings alongside the chosen callers`() =
        assertTrue(rings("700100200", setOf("600100200"), contactsOn = true, savedContact = true))

    @Test fun `a chosen caller rings with contacts on whether or not it is saved`() =
        assertTrue(rings("600100200", setOf("600100200"), contactsOn = true, savedContact = false))

    @Test fun `contacts are not read when a chosen caller already matches`() =
        assertTrue(isAllowedCaller("600100200", setOf("600100200"), true, exact) { error("contacts were read") })

    @Test fun `contacts are not read when they are off`() =
        assertFalse(isAllowedCaller("700100200", setOf("600100200"), false, exact) { error("contacts were read") })

    @Test fun `an entry keeps its name through a round trip`() {
        val caller = ChosenCaller(listOf("+48600100200", "600100200"), "Anna Kowalska")
        assertEquals("+48600100200 600100200\nAnna Kowalska", caller.toEntry())
        assertEquals(caller, ChosenCaller.fromEntry(caller.toEntry()))
    }

    @Test fun `an entry saved before names has no name`() =
        assertEquals(
            ChosenCaller(listOf("+48600100200", "600100200")),
            ChosenCaller.fromEntry("+48600100200 600100200"),
        )

    @Test fun `a caller without a name is stored as its numbers alone`() =
        assertEquals("600100200", ChosenCaller(listOf("600100200")).toEntry())
}
