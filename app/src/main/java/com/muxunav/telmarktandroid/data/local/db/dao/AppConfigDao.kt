package com.muxunav.telmarktandroid.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muxunav.telmarktandroid.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 0")
    fun observe(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 0")
    suspend fun get(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppConfigEntity)
}
