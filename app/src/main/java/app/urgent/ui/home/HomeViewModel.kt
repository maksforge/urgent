package app.urgent.ui.home

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.urgent.MainApplication
import app.urgent.call.ChosenCaller
import app.urgent.data.AppPreferences
import app.urgent.data.CallerRules
import app.urgent.data.ContactScope
import app.urgent.data.DEFAULT_REPEAT_WINDOW_MINUTES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val ringThrough: Boolean = false,
    val keepAlive: Boolean = false,
    val windowMinutes: Int = DEFAULT_REPEAT_WINDOW_MINUTES,
    val countDeclined: Boolean = true,
    val callers: CallerRules = CallerRules(),
)

class HomeViewModel(
    private val preferences: AppPreferences,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        combine(
            preferences.ringThrough,
            preferences.keepAlive,
            preferences.repeatWindowMinutes,
            preferences.countDeclinedCalls,
            preferences.callerRules,
            ::HomeUiState,
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setRingThrough(enabled: Boolean) {
        viewModelScope.launch { preferences.setRingThrough(enabled) }
    }

    fun setKeepAlive(enabled: Boolean) {
        viewModelScope.launch { preferences.setKeepAlive(enabled) }
    }

    fun setWindowMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setRepeatWindowMinutes(minutes) }
    }

    fun setCountDeclined(enabled: Boolean) {
        viewModelScope.launch { preferences.setCountDeclinedCalls(enabled) }
    }

    fun setUseChosenCallers(enabled: Boolean) {
        viewModelScope.launch { preferences.setUseChosenCallers(enabled) }
    }

    fun setContactScope(scope: ContactScope) {
        viewModelScope.launch { preferences.setContactScope(scope) }
    }

    fun addChosenCaller(number: String) {
        viewModelScope.launch { storeTypedCaller(number, preferences::addChosenCaller) }
    }

    fun addRingAtOnceCaller(number: String) {
        viewModelScope.launch { storeTypedCaller(number, preferences::addRingAtOnceCaller) }
    }

    suspend fun contactCallers(): List<PickedCaller> =
        withContext(Dispatchers.IO) { contentResolver.readContactCallers() }

    // One entry per row, so removing the row removes every form of it.
    fun addChosenCallerFromContacts(caller: PickedCaller) {
        viewModelScope.launch { preferences.addChosenCaller(ChosenCaller(caller.forms, caller.name).toEntry()) }
    }

    fun addRingAtOnceCallerFromContacts(caller: PickedCaller) {
        viewModelScope.launch { preferences.addRingAtOnceCaller(ChosenCaller(caller.forms, caller.name).toEntry()) }
    }

    fun removeChosenCaller(entry: String) {
        viewModelScope.launch { preferences.removeChosenCaller(entry) }
    }

    fun removeRingAtOnceCaller(entry: String) {
        viewModelScope.launch { preferences.removeRingAtOnceCaller(entry) }
    }

    private suspend fun storeTypedCaller(
        number: String?,
        add: suspend (String) -> Unit,
    ): Boolean {
        val stored = storedCallerNumber(number) ?: return false
        add(ChosenCaller(listOf(stored)).toEntry())
        return true
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    HomeViewModel(app.container.preferences, app.contentResolver)
                }
            }
    }
}
