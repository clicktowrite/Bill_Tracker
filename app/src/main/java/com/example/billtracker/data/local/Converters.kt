package com.example.billtracker.data.local

import androidx.room.TypeConverter
import com.example.billtracker.domain.model.PaymentMethod
import com.example.billtracker.domain.model.PaymentStatus
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.UtilityType

class Converters {

    @TypeConverter
    fun fromUtilityType(value: UtilityType?): String? = value?.name

    @TypeConverter
    fun toUtilityType(value: String?): UtilityType? = value?.let {
        try { UtilityType.valueOf(it) } catch (_: Exception) { null }
    }

    @TypeConverter
    fun fromRateType(value: RateType?): String? = value?.name

    @TypeConverter
    fun toRateType(value: String?): RateType? = value?.let {
        try { RateType.valueOf(it) } catch (_: Exception) { null }
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let {
        try { PaymentMethod.valueOf(it) } catch (_: Exception) { null }
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus?): String? = value?.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? = value?.let {
        try { PaymentStatus.valueOf(it) } catch (_: Exception) { null }
    }
}
