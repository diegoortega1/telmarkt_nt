package com.muxunav.telmarktandroid.domain.usecase

import com.muxunav.telmarktandroid.domain.model.AppConfig
import com.muxunav.telmarktandroid.domain.repository.AppConfigRepository
import com.muxunav.telmarktandroid.domain.repository.HostRepository
import javax.inject.Inject

class SyncServerInfoUseCase @Inject constructor(
    private val hostRepository: HostRepository,
    private val appConfigRepository: AppConfigRepository,
) {
    /**
     * Sincroniza con el servidor, persiste el resultado y lo devuelve.
     * Lanza excepción si el servidor no responde — el caller decide el fallback.
     */
    suspend operator fun invoke(): AppConfig {
        val config = hostRepository.syncServerInfo()
        appConfigRepository.save(config)
        return config
    }
}
