package app.urgent.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChosenCallerTest {
    private val exact: (String, String) -> Boolean = { a, b -> a == b }
    private val suffix: (String, String) -> Boolean = { a, b -> a.takeLast(7) == b.takeLast(7) }

    @Test fun `anyone rings when no caller is chosen`() = assertTrue(isChosenCaller("600100200", emptySet(), exact))

    @Test fun `a chosen caller rings`() = assertTrue(isChosenCaller("600100200", setOf("600100200"), exact))

    @Test fun `a caller who is not chosen does not ring`() =
        assertFalse(isChosenCaller("700100200", setOf("600100200"), exact))

    @Test fun `a caller with several numbers rings on any of them`() =
        assertTrue(isChosenCaller("+48600100200", setOf("+49711111111 600100200"), suffix))

    @Test fun `a name after the numbers does not change who rings`() =
        assertTrue(isChosenCaller("600100200", setOf("600100200\nAnna Kowalska"), exact))

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
