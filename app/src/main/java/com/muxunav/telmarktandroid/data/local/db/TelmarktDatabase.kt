package com.muxunav.telmarktandroid.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muxunav.telmarktandroid.data.local.db.dao.AppConfigDao
import com.muxunav.telmarktandroid.data.local.entity.AppConfigEntity

@Database(
    entities = [AppConfigEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TelmarktDatabase : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
}
