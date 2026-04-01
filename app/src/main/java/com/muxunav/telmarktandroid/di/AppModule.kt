package com.muxunav.telmarktandroid.di

import android.content.Context
import androidx.room.Room
import com.muxunav.telmarktandroid.data.host.HostRepositoryImpl
import com.muxunav.telmarktandroid.data.local.AppConfigRepositoryImpl
import com.muxunav.telmarktandroid.data.local.db.TelmarktDatabase
import com.muxunav.telmarktandroid.data.local.db.dao.AppConfigDao
import com.muxunav.telmarktandroid.data.mdb.MdbRepositoryImpl
import com.muxunav.telmarktandroid.domain.repository.AppConfigRepository
import com.muxunav.telmarktandroid.domain.repository.HostRepository
import com.muxunav.telmarktandroid.domain.repository.MdbRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindMdbRepository(impl: MdbRepositoryImpl): MdbRepository

    @Binds @Singleton
    abstract fun bindHostRepository(impl: HostRepositoryImpl): HostRepository

    @Binds @Singleton
    abstract fun bindAppConfigRepository(impl: AppConfigRepositoryImpl): AppConfigRepository

    companion object {

        @Provides @Singleton @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides @Singleton
        fun provideTelmarktDatabase(@ApplicationContext context: Context): TelmarktDatabase =
            Room.databaseBuilder(context, TelmarktDatabase::class.java, "telmarkt.db").build()

        @Provides
        fun provideAppConfigDao(db: TelmarktDatabase): AppConfigDao = db.appConfigDao()
    }
}
