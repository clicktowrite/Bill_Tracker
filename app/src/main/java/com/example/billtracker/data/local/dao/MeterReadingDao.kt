package com.example.billtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.billtracker.data.local.entity.MeterReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterReadingDao {

    @Query("SELECT * FROM meter_readings ORDER BY billingDate DESC")
    fun getAllReadingsFlow(): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE meterId = :meterId ORDER BY billingDate DESC")
    fun getReadingsForMeterFlow(meterId: Long): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE tenantId = :tenantId ORDER BY billingDate DESC")
    fun getReadingsForTenantFlow(tenantId: Long): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE meterId = :meterId ORDER BY billingDate DESC LIMIT 1")
    suspend fun getLatestReadingForMeter(meterId: Long): MeterReadingEntity?

    @Query("SELECT * FROM meter_readings WHERE id = :id")
    suspend fun getReadingById(id: Long): MeterReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReadingEntity): Long

    @Update
    suspend fun updateReading(reading: MeterReadingEntity)

    @Query("UPDATE meter_readings SET isPaid = :isPaid WHERE id = :readingId")
    suspend fun updatePaymentStatus(readingId: Long, isPaid: Boolean)

    @Delete
    suspend fun deleteReading(reading: MeterReadingEntity)

    @Query("DELETE FROM meter_readings WHERE id = :id")
    suspend fun deleteReadingById(id: Long)
}
