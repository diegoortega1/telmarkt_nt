package com.muxunav.telmarktandroid.domain.repository

import com.muxunav.telmarktandroid.domain.model.AppConfig
import kotlinx.coroutines.flow.Flow

interface AppConfigRepository {
    /** Flow reactivo: emite cada vez que la config se actualiza en base de datos. */
    fun observe(): Flow<AppConfig?>

    /** Persiste la config localmente. */
    suspend fun save(config: AppConfig)

    /**
     * Devuelve la última config guardada, o [AppConfig.DEFAULT] si no hay ninguna.
     * Nunca lanza — garantiza que el arranque siempre tiene una config válida.
     */
    suspend fun getLastOrDefault(): AppConfig
}
