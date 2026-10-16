package app.urgent.call

import app.urgent.data.CallerRules
import app.urgent.data.ContactScope
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
        useChosen: Boolean = true,
        scope: ContactScope = ContactScope.OFF,
        contact: CallerContact = CallerContact.NONE,
        sameNumber: (String, String) -> Boolean = exact,
    ) = isAllowedCaller(incoming, CallerRules(chosen, useChosen, scope), sameNumber, contact)

    @Test fun `anyone rings when no caller is chosen and contacts are off`() = assertTrue(rings("600100200"))

    @Test fun `a chosen caller rings`() = assertTrue(rings("600100200", setOf("600100200")))

    @Test fun `a caller who is not chosen does not ring`() = assertFalse(rings("700100200", setOf("600100200")))

    @Test fun `a caller with several numbers rings on any of them`() =
        assertTrue(rings("+48600100200", setOf("+49711111111 600100200"), sameNumber = suffix))

    @Test fun `a name after the numbers does not change who rings`() =
        assertTrue(rings("600100200", setOf("600100200\nAnna Kowalska")))

    @Test fun `anyone rings while the chosen list is switched off`() =
        assertTrue(rings("700100200", setOf("600100200"), useChosen = false))

    @Test fun `a chosen caller does not ring through the list while it is switched off`() =
        assertFalse(rings("600100200", setOf("600100200"), useChosen = false, scope = ContactScope.EVERYONE))

    @Test fun `only a saved contact rings when everyone in contacts is on and none are chosen`() {
        assertTrue(rings("600100200", scope = ContactScope.EVERYONE, contact = CallerContact.SAVED))
        assertFalse(rings("700100200", scope = ContactScope.EVERYONE, contact = CallerContact.NONE))
    }

    @Test fun `a saved contact rings alongside the chosen callers`() =
        assertTrue(
            rings("700100200", setOf("600100200"), scope = ContactScope.EVERYONE, contact = CallerContact.SAVED),
        )

    @Test fun `a chosen caller rings with everyone in contacts on when it is not saved`() =
        assertTrue(rings("600100200", setOf("600100200"), scope = ContactScope.EVERYONE))

    @Test fun `with everyone in contacts on, a number that is neither chosen nor saved does not ring`() =
        assertFalse(rings("800100200", setOf("600100200"), scope = ContactScope.EVERYONE))

    @Test fun `a starred contact rings with starred contacts on`() =
        assertTrue(rings("600100200", scope = ContactScope.STARRED, contact = CallerContact.STARRED))

    @Test fun `a saved contact that is not starred does not ring with starred contacts on`() =
        assertFalse(rings("600100200", scope = ContactScope.STARRED, contact = CallerContact.SAVED))

    @Test fun `a starred contact rings alongside the chosen callers`() =
        assertTrue(
            rings("700100200", setOf("600100200"), scope = ContactScope.STARRED, contact = CallerContact.STARRED),
        )

    @Test fun `with contacts off, a saved contact does not ring unless it is chosen`() =
        assertFalse(rings("700100200", setOf("600100200"), contact = CallerContact.SAVED))

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
