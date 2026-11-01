package com.example.billtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.billtracker.domain.model.Payment
import com.example.billtracker.domain.model.PaymentMethod

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = MeterReadingEntity::class,
            parentColumns = ["id"],
            childColumns = ["readingId"],
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
        Index(value = ["readingId"]),
        Index(value = ["tenantId"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val readingId: Long,
    val tenantId: Long? = null,
    val amount: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Payment = Payment(
        id = id,
        readingId = readingId,
        tenantId = tenantId,
        amount = amount,
        paymentDate = paymentDate,
        paymentMethod = paymentMethod,
        referenceNumber = referenceNumber,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(payment: Payment): PaymentEntity = PaymentEntity(
            id = payment.id,
            readingId = payment.readingId,
            tenantId = payment.tenantId,
            amount = payment.amount,
            paymentDate = payment.paymentDate,
            paymentMethod = payment.paymentMethod,
            referenceNumber = payment.referenceNumber,
            notes = payment.notes,
            createdAt = payment.createdAt
        )
    }
}
