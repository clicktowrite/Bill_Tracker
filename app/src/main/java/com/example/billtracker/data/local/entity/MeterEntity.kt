package com.example.billtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.billtracker.domain.model.Meter
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.UtilityType

@Entity(
    tableName = "meters",
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["tenantId"])]
)
data class MeterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val utilityType: UtilityType,
    val tenantId: Long? = null,
    val currentReading: String = "0.0",
    val rateType: RateType = RateType.FIXED,
    val fixedRate: String = "0.00",
    val isForecastEnabled: Boolean = true,
    val isClosed: Boolean = false,
    val parentMeterId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Meter = Meter(
        id = id,
        name = name,
        utilityType = utilityType,
        tenantId = tenantId,
        currentReading = currentReading,
        rateType = rateType,
        fixedRate = fixedRate,
        isForecastEnabled = isForecastEnabled,
        isClosed = isClosed,
        parentMeterId = parentMeterId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(meter: Meter): MeterEntity = MeterEntity(
            id = meter.id,
            name = meter.name,
            utilityType = meter.utilityType,
            tenantId = meter.tenantId,
            currentReading = meter.currentReading,
            rateType = meter.rateType,
            fixedRate = meter.fixedRate,
            isForecastEnabled = meter.isForecastEnabled,
            isClosed = meter.isClosed,
            parentMeterId = meter.parentMeterId,
            createdAt = meter.createdAt
        )
    }
}
