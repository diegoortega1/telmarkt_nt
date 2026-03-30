package com.example.telmarktnt.data.mdb

import com.example.telmarktnt.di.ApplicationScope
import com.example.telmarktnt.domain.model.MdbState
import com.example.telmarktnt.domain.repository.MdbRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MdbRepositoryImpl @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) : MdbRepository {

    private val _state = MutableStateFlow<MdbState>(MdbState.Idle)
    override val state: StateFlow<MdbState> = _state.asStateFlow()

    private var collectJob: Job? = null
    private var service: MdbService? = null

    // Llamado desde MainActivity cuando el ServiceConnection conecta/desconecta
    fun onServiceConnected(mdbService: MdbService) {
        service = mdbService
        collectJob?.cancel()
        collectJob = scope.launch {
            mdbService.state.collect { _state.value = it }
        }
    }

    fun onServiceDisconnected() {
        collectJob?.cancel()
        collectJob = null
        service = null
        _state.value = MdbState.Idle
    }

    override fun beginSession() = service?.beginSession() ?: Unit
    override fun approveVend() = service?.approveVend() ?: Unit
    override fun denyVend()    = service?.denyVend()    ?: Unit
}
