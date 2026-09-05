package com.chronopass.app.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chronopass.app.data.dao.*
import com.chronopass.app.data.entities.*
import java.util.UUID

class Converters {
    @TypeConverter fun toType(v: String) = PunchType.valueOf(v)
    @TypeConverter fun fromType(t: PunchType) = t.name
}

// v1 -> v2: soft-delete columns. Keeps existing data on device.
private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employee ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE punch ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

// v2 -> v3: employee photo (file path in app-private files/employees).
private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employee ADD COLUMN photoPath TEXT")
            }
        }

// v3 -> v4: uid externo (UUID) p/ o Summus + tabela da fila de sync.
// Backfill roda em Kotlin (SELECT + UPDATE por linha) p/ preencher linhas existentes.
private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employee ADD COLUMN uid TEXT")
                db.execSQL("ALTER TABLE punch ADD COLUMN uid TEXT")
                db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `sync_outbox` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tipo` TEXT NOT NULL, `refUid` TEXT, `payload` TEXT NOT NULL, `status` TEXT NOT NULL, `tentativas` INTEGER NOT NULL, `ultimoErro` TEXT, `createdAt` INTEGER NOT NULL)"
                )
                backfillUid(db, "employee")
                backfillUid(db, "punch")
            }
        }

// Ponte: coluna uid é nullable no SQL (Room valida nullability); linhas antigas ganham
// um UUID novo aqui, nunca ficam NULL depois da migração.
private fun backfillUid(db: SupportSQLiteDatabase, table: String) {
    db.query("SELECT id FROM $table WHERE uid IS NULL").use { cursor ->
        val ids = ArrayList<Long>()
        while (cursor.moveToNext()) ids.add(cursor.getLong(0))
        for (id in ids) {
            db.execSQL(
                    "UPDATE $table SET uid = ? WHERE id = ?",
                    arrayOf(UUID.randomUUID().toString(), id)
            )
        }
    }
}

@Database(
        entities =
                [Employee::class, Punch::class, Store::class, AppSetting::class, OutboxItem::class],
        version = 4,
        exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ChronoDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun punchDao(): PunchDao
    abstract fun storeDao(): StoreDao
    abstract fun settingsDao(): SettingsDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile private var instance: ChronoDatabase? = null
        fun get(context: Context): ChronoDatabase =
                instance
                        ?: synchronized(this) {
                            instance
                                    ?: Room.databaseBuilder(
                                                    context.applicationContext,
                                                    ChronoDatabase::class.java,
                                                    "chronopass.db"
                                            )
                                            .addMigrations(
                                                    MIGRATION_1_2,
                                                    MIGRATION_2_3,
                                                    MIGRATION_3_4
                                            )
                                            .build()
                                            .also { instance = it }
                        }
        fun close() {
            instance?.close()
            instance = null
        }
    }
}
