package com.muxunav.telmarktandroid.domain.model

/**
 * Paso genérico de un proceso secuencial con feedback visual.
 * Reutilizable para startup, inicialización de Redsys, etc.
 */
data class SequentialStep(
    val id: String,
    val label: String,
    val status: StepStatus,
    val errorMessage: String? = null,
) {
    enum class StepStatus { PENDING, RUNNING, DONE, ERROR }
}

sealed class AppStartupResult {
    data class InProgress(val steps: List<SequentialStep>) : AppStartupResult()
    data class Completed(val config: AppConfig)            : AppStartupResult()
}
