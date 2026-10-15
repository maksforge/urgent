package app.urgent.ui.home

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.urgent.MainApplication
import app.urgent.data.AppPreferences
import app.urgent.data.DEFAULT_REPEAT_WINDOW_MINUTES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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

    // A Channel, not a StateFlow: two misses in a row must both reach the screen.
    private val pickMisses = Channel<Unit>(Channel.BUFFERED)
    val pickMissed: Flow<Unit> = pickMisses.receiveAsFlow()

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

    fun addChosenCallerFromContacts(pickedUri: Uri) {
        viewModelScope.launch {
            val number = withContext(Dispatchers.IO) { contentResolver.pickedNumber(pickedUri) }
            if (!storeChosenCaller(number)) pickMisses.trySend(Unit)
        }
    }

    fun removeChosenCaller(number: String) {
        viewModelScope.launch { preferences.removeChosenCaller(number) }
    }

    private suspend fun storeChosenCaller(number: String?): Boolean {
        val stored = storedCallerNumber(number) ?: return false
        preferences.addChosenCaller(stored)
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
