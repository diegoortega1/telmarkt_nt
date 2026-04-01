package com.muxunav.telmarktandroid.data.local

import com.muxunav.telmarktandroid.data.local.db.dao.AppConfigDao
import com.muxunav.telmarktandroid.data.local.entity.AppConfigEntity
import com.muxunav.telmarktandroid.domain.model.AppConfig
import com.muxunav.telmarktandroid.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppConfigRepositoryImpl @Inject constructor(
    private val dao: AppConfigDao,
) : AppConfigRepository {

    override fun observe(): Flow<AppConfig?> = dao.observe().map { it?.toDomain() }

    override suspend fun save(config: AppConfig) = dao.upsert(config.toEntity())

    override suspend fun getLastOrDefault(): AppConfig = dao.get()?.toDomain() ?: AppConfig.DEFAULT
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun AppConfigEntity.toDomain() = AppConfig(
    paymentMode        = AppConfig.PaymentMode.valueOf(paymentMode),
    ageControlMethod   = AppConfig.AgeControlMethod.valueOf(ageControlMethod),
    nextCommMinutes    = nextCommMinutes,
    mdbInUse           = mdbInUse,
    useMdbLevel3       = useMdbLevel3,
    maxMobileCredit    = maxMobileCredit,
    rebootTime         = rebootTime,
    updateProductsNeeded = updateProductsNeeded,
    updatePulsesNeeded = updatePulsesNeeded,
    updateLcdsNeeded   = updateLcdsNeeded,
    lastSyncAt         = lastSyncAt,
)

private fun AppConfig.toEntity() = AppConfigEntity(
    id                 = 0,
    paymentMode        = paymentMode.name,
    ageControlMethod   = ageControlMethod.name,
    nextCommMinutes    = nextCommMinutes,
    mdbInUse           = mdbInUse,
    useMdbLevel3       = useMdbLevel3,
    maxMobileCredit    = maxMobileCredit,
    rebootTime         = rebootTime,
    updateProductsNeeded = updateProductsNeeded,
    updatePulsesNeeded = updatePulsesNeeded,
    updateLcdsNeeded   = updateLcdsNeeded,
    lastSyncAt         = lastSyncAt,
)
