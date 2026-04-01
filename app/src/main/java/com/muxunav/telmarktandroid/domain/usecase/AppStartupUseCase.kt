package com.muxunav.telmarktandroid.domain.usecase

import com.muxunav.telmarktandroid.domain.model.AppConfig
import com.muxunav.telmarktandroid.domain.model.AppStartupResult
import com.muxunav.telmarktandroid.domain.model.SequentialStep
import com.muxunav.telmarktandroid.domain.model.SequentialStep.StepStatus.DONE
import com.muxunav.telmarktandroid.domain.model.SequentialStep.StepStatus.ERROR
import com.muxunav.telmarktandroid.domain.model.SequentialStep.StepStatus.PENDING
import com.muxunav.telmarktandroid.domain.model.SequentialStep.StepStatus.RUNNING
import com.muxunav.telmarktandroid.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AppStartupUseCase @Inject constructor(
    private val syncServerInfoUseCase: SyncServerInfoUseCase,
    private val appConfigRepository: AppConfigRepository,
) {
    operator fun invoke(): Flow<AppStartupResult> = flow {
        var steps = listOf(
            SequentialStep("server_sync", "Sincronizando con servidor", PENDING),
        )

        fun update(id: String, status: SequentialStep.StepStatus, error: String? = null) =
            steps.map { if (it.id == id) it.copy(status = status, errorMessage = error) else it }
                .also { steps = it }

        emit(AppStartupResult.InProgress(update("server_sync", RUNNING)))

        val config: AppConfig = try {
            val c = syncServerInfoUseCase()
            emit(AppStartupResult.InProgress(update("server_sync", DONE)))
            c
        } catch (e: Exception) {
            emit(AppStartupResult.InProgress(update("server_sync", ERROR, e.message)))
            appConfigRepository.getLastOrDefault()
        }

        emit(AppStartupResult.Completed(config))
    }
}
