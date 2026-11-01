package com.example.billtracker.data.backup

import android.content.Context
import android.net.Uri
import com.example.billtracker.data.local.BillTrackerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupValidationResult(
    val isValid: Boolean,
    val tenantCount: Int = 0,
    val meterCount: Int = 0,
    val readingCount: Int = 0,
    val paymentCount: Int = 0,
    val dailyReadingCount: Int = 0,
    val errorMessage: String? = null,
    val jsonContent: String? = null
)

object BackupRestoreManager {

    private const val BACKUP_VERSION = 1

    /**
     * Export complete database to JSON URI selected via SAF.
     */
    suspend fun createBackup(
        context: Context,
        database: BillTrackerDatabase,
        targetUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val tenants = database.tenantDao().getAllTenantsFlow().first()
            val meters = database.meterDao().getAllMetersFlow().first()
            val readings = database.meterReadingDao().getAllReadingsFlow().first()
            val payments = database.paymentDao().getAllPaymentsFlow().first()

            val rootJson = JSONObject()
            rootJson.put("backupVersion", BACKUP_VERSION)
            rootJson.put("timestamp", System.currentTimeMillis())

            // Tenants
            val tenantsArray = JSONArray()
            tenants.forEach { t ->
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("name", t.name)
                obj.put("roomOrUnit", t.roomOrUnit)
                obj.put("notes", t.notes)
                obj.put("createdAt", t.createdAt)
                tenantsArray.put(obj)
            }
            rootJson.put("tenants", tenantsArray)

            // Meters
            val metersArray = JSONArray()
            meters.forEach { m ->
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("name", m.name)
                obj.put("utilityType", m.utilityType.name)
                obj.put("tenantId", m.tenantId)
                obj.put("currentReading", m.currentReading)
                obj.put("rateType", m.rateType.name)
                obj.put("fixedRate", m.fixedRate)
                obj.put("createdAt", m.createdAt)
                metersArray.put(obj)
            }
            rootJson.put("meters", metersArray)

            // Readings
            val readingsArray = JSONArray()
            readings.forEach { r ->
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("meterId", r.meterId)
                obj.put("tenantId", r.tenantId)
                obj.put("billingDate", r.billingDate)
                obj.put("previousReading", r.previousReading)
                obj.put("currentReading", r.currentReading)
                obj.put("usage", r.usage)
                obj.put("rate", r.rate)
                obj.put("rateType", r.rateType.name)
                obj.put("providerTotalBill", r.providerTotalBill)
                obj.put("providerTotalUsage", r.providerTotalUsage)
                obj.put("totalAmount", r.totalAmount)
                obj.put("notes", r.notes)
                obj.put("photoPath", r.photoPath)
                obj.put("isFinalizedBill", r.isFinalizedBill)
                obj.put("dueDate", r.dueDate)
                readingsArray.put(obj)
            }
            rootJson.put("readings", readingsArray)

            // Payments
            val paymentsArray = JSONArray()
            payments.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("readingId", p.readingId)
                obj.put("tenantId", p.tenantId)
                obj.put("amount", p.amount)
                obj.put("paymentDate", p.paymentDate)
                obj.put("paymentMethod", p.paymentMethod.name)
                obj.put("referenceNumber", p.referenceNumber)
                obj.put("notes", p.notes)
                obj.put("createdAt", p.createdAt)
                paymentsArray.put(obj)
            }
            rootJson.put("payments", paymentsArray)

            val outputStream = context.contentResolver.openOutputStream(targetUri) ?: return@withContext false
            outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Validate a backup JSON file before restoration.
     */
    suspend fun validateBackup(
        context: Context,
        sourceUri: Uri
    ): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext BackupValidationResult(false, errorMessage = "Could not open selected file.")

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val rootJson = JSONObject(jsonString)

            if (!rootJson.has("backupVersion")) {
                return@withContext BackupValidationResult(false, errorMessage = "Invalid backup format.")
            }

            val tenants = rootJson.optJSONArray("tenants")?.length() ?: 0
            val meters = rootJson.optJSONArray("meters")?.length() ?: 0
            val readings = rootJson.optJSONArray("readings")?.length() ?: 0
            val payments = rootJson.optJSONArray("payments")?.length() ?: 0

            BackupValidationResult(
                isValid = true,
                tenantCount = tenants,
                meterCount = meters,
                readingCount = readings,
                paymentCount = payments,
                jsonContent = jsonString
            )
        } catch (e: Exception) {
            BackupValidationResult(false, errorMessage = "Backup file is corrupt or invalid: ${e.localizedMessage}")
        }
    }

    /**
     * Restores database from a validated JSON backup.
     */
    suspend fun restoreFromValidatedBackup(
        database: BillTrackerDatabase,
        jsonContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject(jsonContent)

            val tenantsArray = rootJson.optJSONArray("tenants")
            val metersArray = rootJson.optJSONArray("meters")
            val readingsArray = rootJson.optJSONArray("readings")
            val paymentsArray = rootJson.optJSONArray("payments")

            database.runInTransaction {
                // Clear existing tables
                database.clearAllTables()

                // Restore Tenants
                if (tenantsArray != null) {
                    for (i in 0 until tenantsArray.length()) {
                        val obj = tenantsArray.getJSONObject(i)
                        database.openHelper.writableDatabase.execSQL(
                            "INSERT INTO tenants (id, name, roomOrUnit, notes, createdAt) VALUES (?, ?, ?, ?, ?)",
                            arrayOf<Any?>(
                                obj.getLong("id"),
                                obj.getString("name"),
                                obj.getString("roomOrUnit"),
                                if (obj.isNull("notes")) null else obj.getString("notes"),
                                obj.getLong("createdAt")
                            )
                        )
                    }
                }

                // Restore Meters
                if (metersArray != null) {
                    for (i in 0 until metersArray.length()) {
                        val obj = metersArray.getJSONObject(i)
                        database.openHelper.writableDatabase.execSQL(
                            "INSERT INTO meters (id, name, utilityType, tenantId, currentReading, rateType, fixedRate, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(
                                obj.getLong("id"),
                                obj.getString("name"),
                                obj.getString("utilityType"),
                                if (obj.isNull("tenantId")) null else obj.getLong("tenantId"),
                                obj.getString("currentReading"),
                                obj.getString("rateType"),
                                obj.getString("fixedRate"),
                                obj.getLong("createdAt")
                            )
                        )
                    }
                }

                // Restore Readings
                if (readingsArray != null) {
                    for (i in 0 until readingsArray.length()) {
                        val obj = readingsArray.getJSONObject(i)
                        database.openHelper.writableDatabase.execSQL(
                            "INSERT INTO meter_readings (id, meterId, tenantId, billingDate, previousReading, currentReading, usage, rate, rateType, providerTotalBill, providerTotalUsage, totalAmount, notes, photoPath, isFinalizedBill, dueDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(
                                obj.getLong("id"),
                                obj.getLong("meterId"),
                                if (obj.isNull("tenantId")) null else obj.getLong("tenantId"),
                                obj.getLong("billingDate"),
                                obj.getString("previousReading"),
                                obj.getString("currentReading"),
                                obj.getString("usage"),
                                obj.getString("rate"),
                                obj.getString("rateType"),
                                if (obj.isNull("providerTotalBill")) null else obj.getString("providerTotalBill"),
                                if (obj.isNull("providerTotalUsage")) null else obj.getString("providerTotalUsage"),
                                obj.getString("totalAmount"),
                                if (obj.isNull("notes")) null else obj.getString("notes"),
                                if (obj.isNull("photoPath")) null else obj.getString("photoPath"),
                                if (obj.optBoolean("isFinalizedBill", true)) 1 else 0,
                                if (obj.isNull("dueDate")) null else obj.getLong("dueDate")
                            )
                        )
                    }
                }

                // Restore Payments
                if (paymentsArray != null) {
                    for (i in 0 until paymentsArray.length()) {
                        val obj = paymentsArray.getJSONObject(i)
                        database.openHelper.writableDatabase.execSQL(
                            "INSERT INTO payments (id, readingId, tenantId, amount, paymentDate, paymentMethod, referenceNumber, notes, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(
                                obj.getLong("id"),
                                obj.getLong("readingId"),
                                if (obj.isNull("tenantId")) null else obj.getLong("tenantId"),
                                obj.getString("amount"),
                                obj.getLong("paymentDate"),
                                obj.getString("paymentMethod"),
                                if (obj.isNull("referenceNumber")) null else obj.getString("referenceNumber"),
                                if (obj.isNull("notes")) null else obj.getString("notes"),
                                obj.getLong("createdAt")
                            )
                        )
                    }
                }
            }

            true
        } catch (_: Exception) {
            false
        }
    }
}
