package com.nexta.di

import android.content.Context
import androidx.room.Room
import com.nexta.data.local.AppDatabase
import com.nexta.data.local.EventDao
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
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideEventDao(
        database: AppDatabase
    ): EventDao {
        return database.eventDao()
    }
}
