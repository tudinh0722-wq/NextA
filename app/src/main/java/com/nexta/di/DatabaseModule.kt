package com.nexta.di

import android.content.Context
import androidx.room.Room
import com.nexta.data.local.AlarmDao
import com.nexta.data.local.AppDatabase
import com.nexta.data.local.DatabaseMigrations
import com.nexta.data.local.EventDao
import com.nexta.data.local.EventScheduleDao
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
            .addMigrations(DatabaseMigrations.MIGRATION_2_3)
            // Mọi path không có migration (vd: version 1 cũ còn sót)
            // → xóa DB và tạo lại thay vì crash.
            // Sau khi release chính thức: xóa dòng này và viết đủ migration.
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()

    @Provides
    fun provideAlarmDao(database: AppDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun provideEventScheduleDao(database: AppDatabase): EventScheduleDao = database.eventScheduleDao()
}
