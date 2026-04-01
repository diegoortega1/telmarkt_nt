package com.muxunav.telmarktandroid.domain.repository

import com.muxunav.telmarktandroid.domain.model.AppConfig

interface HostRepository {
    /**
     * Envía ServerInfoRequest al servidor y devuelve la configuración parseada
     * como modelo de dominio. Lanza excepción si la comunicación falla.
     */
    suspend fun syncServerInfo(): AppConfig
}
