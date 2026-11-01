package com.example.billtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.billtracker.data.local.entity.DailyReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReadingDao {

    @Query("SELECT * FROM daily_readings WHERE meterId = :meterId ORDER BY readingDate ASC")
    fun getDailyReadingsForMeterFlow(meterId: Long): Flow<List<DailyReadingEntity>>

    @Query("SELECT * FROM daily_readings WHERE id = :id")
    suspend fun getDailyReadingById(id: Long): DailyReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReading(reading: DailyReadingEntity): Long

    @Update
    suspend fun updateDailyReading(reading: DailyReadingEntity)

    @Delete
    suspend fun deleteDailyReading(reading: DailyReadingEntity)

    @Query("DELETE FROM daily_readings WHERE id = :id")
    suspend fun deleteDailyReadingById(id: Long)
}
