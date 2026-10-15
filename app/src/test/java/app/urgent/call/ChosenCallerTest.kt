package app.urgent.call

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChosenCallerTest {
    private val exact: (String, String) -> Boolean = { a, b -> a == b }

    @Test fun `anyone rings when no caller is chosen`() = assertTrue(isChosenCaller("600100200", emptySet(), exact))

    @Test fun `a chosen caller rings`() = assertTrue(isChosenCaller("600100200", setOf("600100200"), exact))

    @Test fun `a caller who is not chosen does not ring`() =
        assertFalse(isChosenCaller("700100200", setOf("600100200"), exact))
}
