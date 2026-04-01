package com.muxunav.telmarktandroid.presentation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muxunav.telmarktandroid.di.ApplicationScope
import com.muxunav.telmarktandroid.domain.model.AppConfig
import com.muxunav.telmarktandroid.domain.model.AppStartupResult
import com.muxunav.telmarktandroid.domain.model.SequentialStep
import com.muxunav.telmarktandroid.domain.usecase.AppStartupUseCase
import com.muxunav.telmarktandroid.domain.usecase.SyncServerInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val appStartupUseCase: AppStartupUseCase,
    private val syncServerInfoUseCase: SyncServerInfoUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Running(val steps: List<SequentialStep>) : UiState()
        data class Done(val config: AppConfig) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appStartupUseCase().collect { result ->
                when (result) {
                    is AppStartupResult.InProgress -> _uiState.value = UiState.Running(result.steps)
                    is AppStartupResult.Completed  -> {
                        _uiState.value = UiState.Done(result.config)
                        schedulePeriodicSync(result.config.nextCommMinutes)
                    }
                }
            }
        }
    }

    /**
     * Lanza el sync periódico en el ApplicationScope para que sobreviva
     * rotaciones y navegación entre pantallas.
     * La coroutine vive mientras la app esté viva — si el sync falla, lo
     * ignora silenciosamente y reintenta en el siguiente ciclo.
     */
    private fun schedulePeriodicSync(nextCommMinutes: Int) {
        appScope.launch {
            while (true) {
                delay(nextCommMinutes * 60_000L)
                runCatching { syncServerInfoUseCase() }
            }
        }
    }
}
