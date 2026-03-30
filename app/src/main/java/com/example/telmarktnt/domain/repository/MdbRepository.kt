package com.example.telmarktnt.domain.repository

import com.example.telmarktnt.domain.model.MdbState
import kotlinx.coroutines.flow.StateFlow

interface MdbRepository {
    val state: StateFlow<MdbState>
    fun beginSession()
    fun approveVend()
    fun denyVend()
}
