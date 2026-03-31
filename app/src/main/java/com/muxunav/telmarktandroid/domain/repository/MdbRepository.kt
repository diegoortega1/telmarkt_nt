package com.muxunav.telmarktandroid.domain.repository

import com.muxunav.telmarktandroid.domain.model.MdbState
import kotlinx.coroutines.flow.StateFlow

interface MdbRepository {
    val state: StateFlow<MdbState>
    fun beginSession()
    fun approveVend()
    fun denyVend()
}
