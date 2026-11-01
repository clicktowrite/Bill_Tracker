package com.example.billtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.RateType

@Entity(
    tableName = "meter_readings",
    foreignKeys = [
        ForeignKey(
            entity = MeterEntity::class,
            parentColumns = ["id"],
            childColumns = ["meterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["meterId"]),
        Index(value = ["tenantId"])
    ]
)
data class MeterReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterId: Long,
    val tenantId: Long? = null,
    val billingDate: Long = System.currentTimeMillis(),
    val previousReading: String,
    val currentReading: String,
    val usage: String,
    val rate: String,
    val rateType: RateType,
    val providerTotalBill: String? = null,
    val providerTotalUsage: String? = null,
    val totalAmount: String,
    val notes: String? = null,
    val photoPath: String? = null,
    val isFinalizedBill: Boolean = true,
    val dueDate: Long? = null,
    val hasTime: Boolean = false,
    val isMeterReset: Boolean = false,
    val isPaid: Boolean = false
) {
    fun toDomain(): MeterReading = MeterReading(
        id = id,
        meterId = meterId,
        tenantId = tenantId,
        billingDate = billingDate,
        previousReading = previousReading,
        currentReading = currentReading,
        usage = usage,
        rate = rate,
        rateType = rateType,
        providerTotalBill = providerTotalBill,
        providerTotalUsage = providerTotalUsage,
        totalAmount = totalAmount,
        notes = notes,
        photoPath = photoPath,
        isFinalizedBill = isFinalizedBill,
        dueDate = dueDate,
        hasTime = hasTime,
        isMeterReset = isMeterReset,
        isPaid = isPaid
    )

    companion object {
        fun fromDomain(reading: MeterReading): MeterReadingEntity = MeterReadingEntity(
            id = reading.id,
            meterId = reading.meterId,
            tenantId = reading.tenantId,
            billingDate = reading.billingDate,
            previousReading = reading.previousReading,
            currentReading = reading.currentReading,
            usage = reading.usage,
            rate = reading.rate,
            rateType = reading.rateType,
            providerTotalBill = reading.providerTotalBill,
            providerTotalUsage = reading.providerTotalUsage,
            totalAmount = reading.totalAmount,
            notes = reading.notes,
            photoPath = reading.photoPath,
            isFinalizedBill = reading.isFinalizedBill,
            dueDate = reading.dueDate,
            hasTime = reading.hasTime,
            isMeterReset = reading.isMeterReset,
            isPaid = reading.isPaid
        )
    }
}
