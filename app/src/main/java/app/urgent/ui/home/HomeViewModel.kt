package app.urgent.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.urgent.MainApplication
import app.urgent.data.AppPreferences
import app.urgent.data.DEFAULT_REPEAT_WINDOW_MINUTES
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val ringThrough: Boolean = false,
    val windowMinutes: Int = DEFAULT_REPEAT_WINDOW_MINUTES,
)

class HomeViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        combine(preferences.ringThrough, preferences.repeatWindowMinutes, ::HomeUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setRingThrough(enabled: Boolean) {
        viewModelScope.launch { preferences.setRingThrough(enabled) }
    }

    fun setWindowMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setRepeatWindowMinutes(minutes) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    HomeViewModel(app.container.preferences)
                }
            }
    }
}
