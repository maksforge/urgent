package app.urgent.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Which colour scheme applies, regardless of what the phone is set to. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Everything the app remembers. Settings only — there is no database.
 *
 * **DataStore, not SharedPreferences.** SharedPreferences' `commit()` blocks and its `apply()` can
 * still write on the main thread at the wrong moment; DataStore is `suspend`-only and transactional,
 * so there is no API here that can stall a frame.
 *
 * Kotlin note: every read is a `Flow`, not a value. There is no "current value" to ask for
 * synchronously — the first emission arrives after a disk read. That matters at startup: a screen
 * collecting `themeMode` gets `SYSTEM` for one frame and the stored value on the next, which is a
 * visible flash. [themeModeNow] exists for the one caller that must have the answer *before* the
 * first frame, and it is `suspend` because reading a file is.
 *
 * ## Adding a preference
 *
 * A key, a `Flow` that reads it with a default, and a `suspend` setter. Keep the default in the
 * `Flow` rather than writing it on first launch — a default that has been written to disk cannot be
 * changed in a later version without a migration, and a default that has not can.
 */
class AppPreferences(
    private val store: DataStore<Preferences>,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val RING_THROUGH = booleanPreferencesKey("ring_through")
        val REPEAT_WINDOW_MINUTES = intPreferencesKey("repeat_window_minutes")
        val COUNT_DECLINED_CALLS = booleanPreferencesKey("count_declined_calls")
        val CHOSEN_CALLERS = stringSetPreferencesKey("chosen_callers")
    }

    /**
     * Light, dark or follow-the-system.
     *
     * Unknown stored values fall back to [ThemeMode.SYSTEM] rather than throwing: a preference file
     * survives a downgrade, and a crash loop at startup is a far worse outcome than the wrong theme.
     * This is the opposite of the rule for enums stored in the *database*, where an unknown value
     * means the schema gate missed something and should be loud.
     */
    val themeMode: Flow<ThemeMode> =
        store.data.map { prefs ->
            prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { enumValueOf<ThemeMode>(name) }.getOrNull()
            } ?: ThemeMode.SYSTEM
        }

    /**
     * Whether to take the wallpaper's palette instead of the app's own (ADR-0006).
     *
     * Defaults **off**. With it on, nothing on Android 12+ reads the generated scheme at all, so the
     * app's identity would be invisible on almost every device that runs it.
     */
    val materialYou: Flow<Boolean> = store.data.map { it[Keys.MATERIAL_YOU] ?: false }

    /**
     * Whether a repeated call should ring through a silenced phone.
     *
     * Separate from the phone and call-log permissions on purpose, and both are checked when a call
     * arrives. The permissions answer "may we", this answers "did they ask us to".
     */
    val ringThrough: Flow<Boolean> = store.data.map { it[Keys.RING_THROUGH] ?: false }

    /**
     * How recent the earlier call must be for this one to count as a repeat. 15 is Android's own
     * figure for Do Not Disturb's repeat callers; an unknown stored value falls back to it.
     */
    val repeatWindowMinutes: Flow<Int> =
        store.data.map { prefs ->
            prefs[Keys.REPEAT_WINDOW_MINUTES]?.takeIf { it in REPEAT_WINDOW_CHOICES } ?: DEFAULT_REPEAT_WINDOW_MINUTES
        }

    /**
     * Whether a call the user declined counts as the earlier call. Defaults **on**: a decline is often
     * a reflex at an unknown number, and the second attempt is the one Urgent exists for.
     */
    val countDeclinedCalls: Flow<Boolean> = store.data.map { it[Keys.COUNT_DECLINED_CALLS] ?: true }

    /** The chosen callers, one encoded entry each. Empty means anyone. */
    val chosenCallers: Flow<Set<String>> = store.data.map { it[Keys.CHOSEN_CALLERS] ?: emptySet() }

    /**
     * The theme mode, read once, before any Activity exists.
     *
     * The one place a `suspend` read is worth its cost: `MainApplication.onCreate` needs the answer
     * to call `applyThemeMode` *before* the first window is created, and a `Flow` collected in a
     * composition arrives a frame too late — which is a light flash on every cold start for a user
     * who chose dark.
     */
    suspend fun themeModeNow(): ThemeMode = themeMode.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setMaterialYou(enabled: Boolean) {
        store.edit { it[Keys.MATERIAL_YOU] = enabled }
    }

    suspend fun setRingThrough(enabled: Boolean) {
        store.edit { it[Keys.RING_THROUGH] = enabled }
    }

    suspend fun setRepeatWindowMinutes(minutes: Int) {
        store.edit { it[Keys.REPEAT_WINDOW_MINUTES] = minutes }
    }

    suspend fun setCountDeclinedCalls(enabled: Boolean) {
        store.edit { it[Keys.COUNT_DECLINED_CALLS] = enabled }
    }

    suspend fun addChosenCaller(entry: String) {
        store.edit { it[Keys.CHOSEN_CALLERS] = (it[Keys.CHOSEN_CALLERS] ?: emptySet()) + entry }
    }

    suspend fun removeChosenCaller(entry: String) {
        store.edit { it[Keys.CHOSEN_CALLERS] = (it[Keys.CHOSEN_CALLERS] ?: emptySet()) - entry }
    }
}

const val DEFAULT_REPEAT_WINDOW_MINUTES = 15

val REPEAT_WINDOW_CHOICES = listOf(5, 10, 15, 30)
