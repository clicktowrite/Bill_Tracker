package com.example.billtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.billtracker.domain.model.Tenant

@Entity(tableName = "tenants")
data class TenantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roomOrUnit: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Tenant = Tenant(
        id = id,
        name = name,
        roomOrUnit = roomOrUnit,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(tenant: Tenant): TenantEntity = TenantEntity(
            id = tenant.id,
            name = tenant.name,
            roomOrUnit = tenant.roomOrUnit,
            notes = tenant.notes,
            createdAt = tenant.createdAt
        )
    }
}
