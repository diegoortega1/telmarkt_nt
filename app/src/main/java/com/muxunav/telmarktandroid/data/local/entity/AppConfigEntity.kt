package com.muxunav.telmarktandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fila singleton (id = 0 siempre). Se sobreescribe en cada sync exitoso. */
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 0,
    val paymentMode: String,
    val ageControlMethod: String,
    val nextCommMinutes: Int,
    val mdbInUse: Boolean,
    val useMdbLevel3: Boolean,
    val maxMobileCredit: Int,
    val rebootTime: String?,
    val updateProductsNeeded: Boolean,
    val updatePulsesNeeded: Boolean,
    val updateLcdsNeeded: Boolean,
    val lastSyncAt: Long,
)
