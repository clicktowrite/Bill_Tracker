package com.example.billtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.billtracker.data.local.entity.MeterEntity
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {

    @Query("SELECT * FROM meters WHERE isClosed = 0 ORDER BY name ASC")
    fun getAllMetersFlow(): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters ORDER BY isClosed ASC, name ASC")
    fun getAllMetersIncludingClosedFlow(): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE utilityType = :utilityType AND isClosed = 0 ORDER BY name ASC")
    fun getMetersByUtilityTypeFlow(utilityType: UtilityType): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE tenantId = :tenantId AND isClosed = 0 ORDER BY name ASC")
    fun getMetersForTenantFlow(tenantId: Long): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE tenantId = :tenantId ORDER BY isClosed ASC, name ASC")
    fun getAllMetersForTenantFlow(tenantId: Long): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE id = :id")
    suspend fun getMeterById(id: Long): MeterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeter(meter: MeterEntity): Long

    @Update
    suspend fun updateMeter(meter: MeterEntity)

    @Delete
    suspend fun deleteMeter(meter: MeterEntity)

    @Query("DELETE FROM meters WHERE id = :id")
    suspend fun deleteMeterById(id: Long)

    @Query("UPDATE meters SET currentReading = :newReading WHERE id = :meterId")
    suspend fun updateCurrentReading(meterId: Long, newReading: String)

    @Query("UPDATE meters SET isClosed = 1, currentReading = :finalReading WHERE id = :meterId AND isClosed = 0")
    suspend fun closeMeterPeriod(meterId: Long, finalReading: String): Int
}
