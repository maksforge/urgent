package app.urgent.call

/**
 * A chosen caller as DataStore keeps it: its numbers separated by spaces and, after a newline, the name
 * the contact had. An entry with no newline is a caller with no name, which is every entry saved before
 * names. The picker collapses whitespace in a name, so a name never holds the newline.
 */
data class ChosenCaller(
    val numbers: List<String>,
    val name: String? = null,
) {
    val label: String
        get() = name ?: numbers.firstOrNull().orEmpty()

    fun toEntry(): String = numbers.joinToString(" ") + name?.let { "\n$it" }.orEmpty()

    // Kotlin: a companion object is where static-style functions live, as in ChosenCaller.fromEntry(...).
    companion object {
        fun fromEntry(entry: String): ChosenCaller =
            ChosenCaller(
                numbers = entry.substringBefore('\n').split(' ').filter(String::isNotEmpty),
                name = entry.substringAfter('\n', missingDelimiterValue = "").ifEmpty { null },
            )
    }
}
