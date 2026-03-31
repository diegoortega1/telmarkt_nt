package com.muxunav.telmarktandroid.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muxunav.telmarktandroid.domain.model.MdbState
import com.muxunav.telmarktandroid.domain.repository.MdbRepository
import com.muxunav.telmarktandroid.domain.usecase.ApproveVendUseCase
import com.muxunav.telmarktandroid.domain.usecase.BeginSessionUseCase
import com.muxunav.telmarktandroid.domain.usecase.DenyVendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val VEND_SUCCESS_DISPLAY_MS = 5_000L
private const val VEND_DENIED_DISPLAY_MS = 6_000L

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MdbRepository,
    private val beginSessionUseCase: BeginSessionUseCase,
    private val approveVendUseCase: ApproveVendUseCase,
    private val denyVendUseCase: DenyVendUseCase,
) : ViewModel() {

    // Estado hardware real — refleja el Service directamente
    private val _hardwareState = MutableStateFlow<MdbState>(MdbState.Idle)

    // Estado que ve la UI — puede diferir durante los delays de display
    private val _uiState = MutableStateFlow<MdbState>(MdbState.Idle)
    val uiState: StateFlow<MdbState> = _uiState.asStateFlow()

    init {
        // Corrutina 1: sincroniza _hardwareState con el repositorio
        viewModelScope.launch {
            repository.state.collect { _hardwareState.value = it }
        }

        // Corrutina 2: propaga al estado UI con delays para VendSuccess y VendDenied
        viewModelScope.launch {
            _hardwareState.collect { state ->
                when (state) {
                    is MdbState.VendSuccess -> {
                        _uiState.value = MdbState.VendSuccess
                        delay(VEND_SUCCESS_DISPLAY_MS)
                        _uiState.value = _hardwareState.value
                    }
                    is MdbState.VendDenied -> {
                        _uiState.value = MdbState.VendDenied
                        delay(VEND_DENIED_DISPLAY_MS)
                        _uiState.value = _hardwareState.value
                    }
                    else -> {
                        val uiIsLocked = _uiState.value is MdbState.VendSuccess
                                || _uiState.value is MdbState.VendDenied
                        if (!uiIsLocked) _uiState.value = state
                    }
                }
            }
        }
    }

    fun startSession() = beginSessionUseCase()
    fun approveVend() = approveVendUseCase()
    fun denyVend() = denyVendUseCase()
}
