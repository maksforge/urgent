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
    val windowMinutes: Int = DEFAULT_REPEAT_WINDOW_MINUTES,
    val countDeclined: Boolean = true,
    val chosenCallers: Set<String> = emptySet(),
)

class HomeViewModel(
    private val preferences: AppPreferences,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        combine(
            preferences.ringThrough,
            preferences.repeatWindowMinutes,
            preferences.countDeclinedCalls,
            preferences.chosenCallers,
            ::HomeUiState,
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setRingThrough(enabled: Boolean) {
        viewModelScope.launch { preferences.setRingThrough(enabled) }
    }

    fun setWindowMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setRepeatWindowMinutes(minutes) }
    }

    fun setCountDeclined(enabled: Boolean) {
        viewModelScope.launch { preferences.setCountDeclinedCalls(enabled) }
    }

    fun addChosenCaller(number: String) {
        viewModelScope.launch { storeChosenCaller(number) }
    }

    suspend fun contactCallers(): List<PickedCaller> =
        withContext(Dispatchers.IO) { contentResolver.readContactCallers() }

    fun addChosenCallerFromContacts(caller: PickedCaller) {
        viewModelScope.launch {
            // One entry per row, so removing the row removes every form of it.
            preferences.addChosenCaller(ChosenCaller(caller.forms, caller.name).toEntry())
        }
    }

    fun removeChosenCaller(entry: String) {
        viewModelScope.launch { preferences.removeChosenCaller(entry) }
    }

    private suspend fun storeChosenCaller(number: String?): Boolean {
        val stored = storedCallerNumber(number) ?: return false
        preferences.addChosenCaller(ChosenCaller(listOf(stored)).toEntry())
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
