package com.example.billtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.billtracker.data.local.dao.DailyReadingDao
import com.example.billtracker.data.local.dao.MeterDao
import com.example.billtracker.data.local.dao.MeterReadingDao
import com.example.billtracker.data.local.dao.PaymentDao
import com.example.billtracker.data.local.dao.TenantDao
import com.example.billtracker.data.local.entity.DailyReadingEntity
import com.example.billtracker.data.local.entity.MeterEntity
import com.example.billtracker.data.local.entity.MeterReadingEntity
import com.example.billtracker.data.local.entity.PaymentEntity
import com.example.billtracker.data.local.entity.TenantEntity

@Database(
    entities = [
        TenantEntity::class,
        MeterEntity::class,
        MeterReadingEntity::class,
        PaymentEntity::class,
        DailyReadingEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BillTrackerDatabase : RoomDatabase() {

    abstract fun tenantDao(): TenantDao
    abstract fun meterDao(): MeterDao
    abstract fun meterReadingDao(): MeterReadingDao
    abstract fun paymentDao(): PaymentDao
    abstract fun dailyReadingDao(): DailyReadingDao

    companion object {
        @Volatile
        private var INSTANCE: BillTrackerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meter_readings ADD COLUMN photoPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE meter_readings ADD COLUMN isFinalizedBill INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE meter_readings ADD COLUMN dueDate INTEGER DEFAULT NULL")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        readingId INTEGER NOT NULL,
                        tenantId INTEGER DEFAULT NULL,
                        amount TEXT NOT NULL,
                        paymentDate INTEGER NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        referenceNumber TEXT DEFAULT NULL,
                        notes TEXT DEFAULT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(readingId) REFERENCES meter_readings(id) ON DELETE CASCADE,
                        FOREIGN KEY(tenantId) REFERENCES tenants(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_readingId ON payments(readingId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_tenantId ON payments(tenantId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_readings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        meterId INTEGER NOT NULL,
                        readingDate INTEGER NOT NULL,
                        readingValue TEXT NOT NULL,
                        photoPath TEXT DEFAULT NULL,
                        notes TEXT DEFAULT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(meterId) REFERENCES meters(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_readings_meterId ON daily_readings(meterId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meters ADD COLUMN isForecastEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE daily_readings ADD COLUMN hasTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meter_readings ADD COLUMN hasTime INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_readings ADD COLUMN isMeterReset INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meter_readings ADD COLUMN isMeterReset INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meters ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meters ADD COLUMN parentMeterId INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meter_readings ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): BillTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillTrackerDatabase::class.java,
                    "bill_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
