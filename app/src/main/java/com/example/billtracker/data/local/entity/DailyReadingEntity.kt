package com.example.billtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.billtracker.domain.model.DailyReading

@Entity(
    tableName = "daily_readings",
    foreignKeys = [
        ForeignKey(
            entity = MeterEntity::class,
            parentColumns = ["id"],
            childColumns = ["meterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meterId"])
    ]
)
data class DailyReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterId: Long,
    val readingDate: Long = System.currentTimeMillis(),
    val readingValue: String,
    val hasTime: Boolean = false,
    val isMeterReset: Boolean = false,
    val photoPath: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DailyReading = DailyReading(
        id = id,
        meterId = meterId,
        readingDate = readingDate,
        readingValue = readingValue,
        hasTime = hasTime,
        isMeterReset = isMeterReset,
        photoPath = photoPath,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(reading: DailyReading): DailyReadingEntity = DailyReadingEntity(
            id = reading.id,
            meterId = reading.meterId,
            readingDate = reading.readingDate,
            readingValue = reading.readingValue,
            hasTime = reading.hasTime,
            isMeterReset = reading.isMeterReset,
            photoPath = reading.photoPath,
            notes = reading.notes,
            createdAt = reading.createdAt
        )
    }
}
