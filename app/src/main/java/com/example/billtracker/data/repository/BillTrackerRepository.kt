package com.example.billtracker.data.repository

import androidx.room.withTransaction
import com.example.billtracker.data.local.BillTrackerDatabase
import com.example.billtracker.data.local.entity.DailyReadingEntity
import com.example.billtracker.data.local.entity.MeterEntity
import com.example.billtracker.data.local.entity.MeterReadingEntity
import com.example.billtracker.data.local.entity.PaymentEntity
import com.example.billtracker.data.local.entity.TenantEntity
import com.example.billtracker.data.local.preferences.UserPreferencesRepository
import com.example.billtracker.domain.calculator.BillingCalculator
import com.example.billtracker.domain.model.DailyReading
import com.example.billtracker.domain.model.Meter
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.Payment
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.ThemeMode
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BillTrackerRepository(
    private val database: BillTrackerDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val tenantDao = database.tenantDao()
    private val meterDao = database.meterDao()
    private val meterReadingDao = database.meterReadingDao()
    private val paymentDao = database.paymentDao()
    private val dailyReadingDao = database.dailyReadingDao()

    // Preferences
    val themeModeFlow: Flow<ThemeMode> = preferencesRepository.themeModeFlow
    val currencySymbolFlow: Flow<String> = preferencesRepository.currencySymbolFlow
    val recordReadingTimeFlow: Flow<Boolean> = preferencesRepository.recordReadingTimeFlow

    suspend fun setThemeMode(mode: ThemeMode) = preferencesRepository.setThemeMode(mode)
    suspend fun setCurrencySymbol(symbol: String) = preferencesRepository.setCurrencySymbol(symbol)
    suspend fun setRecordReadingTime(enabled: Boolean) = preferencesRepository.setRecordReadingTime(enabled)

    // Tenants
    val allTenantsFlow: Flow<List<Tenant>> = tenantDao.getAllTenantsFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun searchTenants(query: String): Flow<List<Tenant>> = tenantDao.searchTenants(query).map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getTenantById(id: Long): Tenant? = tenantDao.getTenantById(id)?.toDomain()

    suspend fun saveTenant(tenant: Tenant): Long {
        return tenantDao.insertTenant(TenantEntity.fromDomain(tenant))
    }

    suspend fun updateTenant(tenant: Tenant) {
        tenantDao.updateTenant(TenantEntity.fromDomain(tenant))
    }

    suspend fun deleteTenant(id: Long) {
        tenantDao.deleteTenantById(id)
    }

    // Meters
    val allMetersFlow: Flow<List<Meter>> = meterDao.getAllMetersFlow().map { list ->
        list.map { it.toDomain() }
    }

    val allMetersIncludingClosedFlow: Flow<List<Meter>> = meterDao.getAllMetersIncludingClosedFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getMetersByUtilityType(utilityType: UtilityType): Flow<List<Meter>> =
        meterDao.getMetersByUtilityTypeFlow(utilityType).map { list ->
            list.map { it.toDomain() }
        }

    fun getMetersForTenant(tenantId: Long): Flow<List<Meter>> =
        meterDao.getMetersForTenantFlow(tenantId).map { list ->
            list.map { it.toDomain() }
        }

    fun getAllMetersForTenant(tenantId: Long): Flow<List<Meter>> =
        meterDao.getAllMetersForTenantFlow(tenantId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getMeterById(id: Long): Meter? = meterDao.getMeterById(id)?.toDomain()

    suspend fun saveMeter(meter: Meter): Long {
        return meterDao.insertMeter(MeterEntity.fromDomain(meter))
    }

    /**
     * Atomically saves a new meter and automatically records its starting baseline daily reading.
     */
    suspend fun saveMeterWithInitialReading(
        meter: Meter,
        initialReadingDate: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val meterEntity = MeterEntity.fromDomain(meter)
        val meterId = meterDao.insertMeter(meterEntity)

        // Automatically record initial daily reading
        val initialReadingEntity = DailyReadingEntity(
            meterId = meterId,
            readingDate = initialReadingDate,
            readingValue = meter.currentReading,
            hasTime = false,
            isMeterReset = false,
            notes = "Initial Meter Starting Reading"
        )
        dailyReadingDao.insertDailyReading(initialReadingEntity)

        meterId
    }

    suspend fun updateMeter(meter: Meter) {
        meterDao.updateMeter(MeterEntity.fromDomain(meter))
    }

    suspend fun deleteMeter(id: Long) {
        meterDao.deleteMeterById(id)
    }

    // Meter Readings / Bills
    val allReadingsFlow: Flow<List<MeterReading>> = meterReadingDao.getAllReadingsFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getReadingsForMeter(meterId: Long): Flow<List<MeterReading>> =
        meterReadingDao.getReadingsForMeterFlow(meterId).map { list ->
            list.map { it.toDomain() }
        }

    fun getReadingsForTenant(tenantId: Long): Flow<List<MeterReading>> =
        meterReadingDao.getReadingsForTenantFlow(tenantId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getLatestReadingForMeter(meterId: Long): MeterReading? =
        meterReadingDao.getLatestReadingForMeter(meterId)?.toDomain()

    suspend fun getReadingById(id: Long): MeterReading? =
        meterReadingDao.getReadingById(id)?.toDomain()

    suspend fun saveReadingAndUpdateMeter(reading: MeterReading): Long {
        val readingId = meterReadingDao.insertReading(MeterReadingEntity.fromDomain(reading))
        if (reading.isFinalizedBill) {
            meterDao.updateCurrentReading(reading.meterId, reading.currentReading)
        }
        return readingId
    }

    /**
     * Section 3 & 5: Atomically completes current billing cycle:
     * 1. Saves calculated bill.
     * 2. Closes current meter tracking session (isClosed = 1).
     * 3. Creates next active meter session (isClosed = 0) starting from the final reading value.
     * 4. Automatically inserts starting baseline daily reading for the new session.
     * All wrapped inside a single atomic database transaction.
     */
    suspend fun closeTrackingPeriodAndSaveBill(
        reading: MeterReading,
        destinationTenantId: Long?
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val meterId = reading.meterId
            val existingMeter = meterDao.getMeterById(meterId)
                ?: throw IllegalStateException("Meter not found.")

            // 1. Insert finalized bill
            val billId = meterReadingDao.insertReading(MeterReadingEntity.fromDomain(reading))

            // 2. Close current meter tracking session
            meterDao.closeMeterPeriod(meterId, reading.currentReading)

            // 3. Create next active meter tracking session starting at final reading
            val effectiveTenantId = destinationTenantId ?: reading.tenantId
            val newMeterEntity = MeterEntity(
                name = existingMeter.name,
                utilityType = existingMeter.utilityType,
                tenantId = effectiveTenantId,
                currentReading = reading.currentReading,
                rateType = existingMeter.rateType,
                fixedRate = existingMeter.fixedRate,
                isForecastEnabled = existingMeter.isForecastEnabled,
                isClosed = false,
                parentMeterId = existingMeter.parentMeterId ?: existingMeter.id,
                createdAt = System.currentTimeMillis()
            )
            val newMeterId = meterDao.insertMeter(newMeterEntity)

            // 4. Automatically insert baseline starting reading for new session
            val baselineEntity = DailyReadingEntity(
                meterId = newMeterId,
                readingDate = reading.billingDate,
                readingValue = reading.currentReading,
                hasTime = reading.hasTime,
                isMeterReset = false,
                notes = "New Billing Cycle Baseline"
            )
            dailyReadingDao.insertDailyReading(baselineEntity)

            billId
        }
    }

    suspend fun updatePaymentStatus(readingId: Long, isPaid: Boolean) = withContext(Dispatchers.IO) {
        meterReadingDao.updatePaymentStatus(readingId, isPaid)
    }

    suspend fun deleteReading(id: Long) {
        meterReadingDao.deleteReadingById(id)
    }

    // Payments
    val allPaymentsFlow: Flow<List<Payment>> = paymentDao.getAllPaymentsFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getPaymentsForReading(readingId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsForReadingFlow(readingId).map { list ->
            list.map { it.toDomain() }
        }

    fun getPaymentsForTenant(tenantId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsForTenantFlow(tenantId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun savePayment(payment: Payment): Long {
        return paymentDao.insertPayment(PaymentEntity.fromDomain(payment))
    }

    suspend fun deletePayment(id: Long) {
        paymentDao.deletePaymentById(id)
    }

    // Daily Readings
    fun getDailyReadingsForMeter(meterId: Long): Flow<List<DailyReading>> =
        dailyReadingDao.getDailyReadingsForMeterFlow(meterId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun saveDailyReading(reading: DailyReading): Long = withContext(Dispatchers.IO) {
        val parsedNew = BillingCalculator.parseBigDecimal(reading.readingValue)
            ?: throw IllegalArgumentException("Reading value must be a valid non-negative number.")

        val dbReadings = dailyReadingDao.getDailyReadingsForMeterFlow(reading.meterId).first()
        val precedingInDb = dbReadings
            .filter { it.readingDate < reading.readingDate }
            .mapNotNull { r -> BillingCalculator.parseBigDecimal(r.readingValue)?.let { r to it } }
            .maxByOrNull { it.first.readingDate }

        if (precedingInDb != null && !reading.isMeterReset) {
            val precedingVal = precedingInDb.second
            if (parsedNew < precedingVal) {
                throw IllegalArgumentException("Reading ($parsedNew) cannot be lower than the latest saved reading of $precedingVal.")
            }
        }

        val id = dailyReadingDao.insertDailyReading(DailyReadingEntity.fromDomain(reading))
        meterDao.updateCurrentReading(reading.meterId, reading.readingValue)
        id
    }

    suspend fun updateDailyReading(reading: DailyReading) = withContext(Dispatchers.IO) {
        val parsedNew = BillingCalculator.parseBigDecimal(reading.readingValue)
            ?: throw IllegalArgumentException("Reading value must be a valid non-negative number.")

        val dbReadings = dailyReadingDao.getDailyReadingsForMeterFlow(reading.meterId).first()
            .filter { it.id != reading.id }

        val precedingInDb = dbReadings
            .filter { it.readingDate < reading.readingDate }
            .mapNotNull { r -> BillingCalculator.parseBigDecimal(r.readingValue)?.let { r to it } }
            .maxByOrNull { it.first.readingDate }

        if (precedingInDb != null && !reading.isMeterReset) {
            val precedingVal = precedingInDb.second
            if (parsedNew < precedingVal) {
                throw IllegalArgumentException("Reading ($parsedNew) cannot be lower than the preceding saved reading of $precedingVal.")
            }
        }

        val followingInDb = dbReadings
            .filter { it.readingDate > reading.readingDate }
            .mapNotNull { r -> BillingCalculator.parseBigDecimal(r.readingValue)?.let { r to it } }
            .minByOrNull { it.first.readingDate }

        if (followingInDb != null && !followingInDb.first.isMeterReset) {
            val followingVal = followingInDb.second
            if (parsedNew > followingVal) {
                throw IllegalArgumentException("Reading ($parsedNew) cannot be higher than the subsequent saved reading of $followingVal.")
            }
        }

        dailyReadingDao.updateDailyReading(DailyReadingEntity.fromDomain(reading))
        val latestReading = dailyReadingDao.getDailyReadingsForMeterFlow(reading.meterId)
            .first().maxByOrNull { it.readingDate }
        if (latestReading != null) {
            meterDao.updateCurrentReading(reading.meterId, latestReading.readingValue)
        }
    }

    suspend fun deleteDailyReading(id: Long) {
        dailyReadingDao.deleteDailyReadingById(id)
    }

    /**
     * Closes current meter tracking period safely and opens a new active tracking period
     * for the destination profile using the same physical meter and starting baseline.
     * Idempotent and executed inside a database transaction.
     */
    suspend fun closeTrackingPeriodAndTransfer(
        meterId: Long,
        finalReadingValue: String,
        readingDate: Long,
        hasTime: Boolean,
        notes: String?,
        destinationTenantId: Long?
    ): Long = withContext(Dispatchers.IO) {
        val existingEntity = meterDao.getMeterById(meterId)
            ?: throw IllegalStateException("Meter not found.")

        if (existingEntity.isClosed) {
            throw IllegalStateException("Tracking period is already closed.")
        }

        var newMeterId = -1L

        // 1. Save final actual daily reading for current period
        val finalReadingEntity = DailyReadingEntity(
            meterId = meterId,
            readingDate = readingDate,
            readingValue = finalReadingValue,
            hasTime = hasTime,
            isMeterReset = false,
            notes = "Tracking Period Closed: ${notes ?: ""}".trim()
        )
        dailyReadingDao.insertDailyReading(finalReadingEntity)

        // 2. Close current meter period
        meterDao.closeMeterPeriod(meterId, finalReadingValue)

        // 3. Create new active meter tracking session
        val newMeterEntity = MeterEntity(
            name = existingEntity.name,
            utilityType = existingEntity.utilityType,
            tenantId = destinationTenantId,
            currentReading = finalReadingValue,
            rateType = existingEntity.rateType,
            fixedRate = existingEntity.fixedRate,
            isForecastEnabled = existingEntity.isForecastEnabled,
            isClosed = false,
            parentMeterId = existingEntity.parentMeterId ?: existingEntity.id,
            createdAt = System.currentTimeMillis()
        )
        newMeterId = meterDao.insertMeter(newMeterEntity)

        // 4. Save initial baseline reading for new tracking period
        val baselineEntity = DailyReadingEntity(
            meterId = newMeterId,
            readingDate = readingDate,
            readingValue = finalReadingValue,
            hasTime = hasTime,
            isMeterReset = false,
            notes = "New Tracking Period Baseline"
        )
        dailyReadingDao.insertDailyReading(baselineEntity)

        newMeterId
    }
}
