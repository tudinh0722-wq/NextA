package com.nexta.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events") fun getAll(): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE id = :id") suspend fun getById(id: String): EventEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(event: EventEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(events: List<EventEntity>)
    @Update suspend fun update(event: EventEntity)
    @Query("DELETE FROM events WHERE id = :id") suspend fun delete(id: String)
}
