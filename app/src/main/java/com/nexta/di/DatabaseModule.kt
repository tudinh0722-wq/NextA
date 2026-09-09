package com.nexta.di

import android.content.Context
import androidx.room.Room
import com.nexta.data.local.AlarmDao
import com.nexta.data.local.AppDatabase
import com.nexta.data.local.EventDao
import com.nexta.data.local.EventScheduleDao
import com.nexta.data.local.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nexta_db"
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_1_3,
                DatabaseMigrations.MIGRATION_2_3,
            )
            .build()
    }

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()

    @Provides
    fun provideAlarmDao(database: AppDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun provideEventScheduleDao(database: AppDatabase): EventScheduleDao = database.eventScheduleDao()
}
