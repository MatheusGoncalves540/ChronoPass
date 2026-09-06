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

// v4 -> v5: recepção do Summus (plano Fase 3). NENHUMA linha é apagada aqui: o saneamento de
// uid preenche nulos e resolve duplicata REATRIBUINDO uid, nunca com DELETE.
private val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employee ADD COLUMN role TEXT")
                db.execSQL(
                        "ALTER TABLE employee ADD COLUMN origin TEXT NOT NULL DEFAULT 'LOCAL'"
                )
                db.execSQL("ALTER TABLE store ADD COLUMN uid TEXT")
                db.execSQL(
                        "ALTER TABLE store ADD COLUMN managedBySummus INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE punch ADD COLUMN serverRevision INTEGER")
                db.execSQL("ALTER TABLE sync_outbox ADD COLUMN lastAttemptAt INTEGER")
                // Saneamento ANTES do índice único: no SQLite UNIQUE tolera N NULLs, então o
                // índice sozinho deixaria linha legada sem uid p/ sempre e estouraria em
                // duplicata legada (restore de backup antigo).
                sanearUid(db, "employee")
                sanearUid(db, "punch")
                // Nome idêntico ao que o Room gera p/ @Index(value=["uid"], unique=true).
                db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_employee_uid` ON `employee` (`uid`)"
                )
                db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_punch_uid` ON `punch` (`uid`)"
                )
            }
        }

// Ponte: coluna uid é nullable no SQL (Room valida nullability); linhas antigas ganham
// um UUID novo aqui, nunca ficam NULL depois da migração.
private fun backfillUid(db: SupportSQLiteDatabase, table: String) =
        novoUid(db, table, ids(db, "SELECT id FROM $table WHERE uid IS NULL"))

// Pré-requisito do índice único. Duplicata legada: a linha mais ANTIGA (menor id) mantém o uid
// que o servidor já conhece; as demais ganham uid novo. Reatribuição, nunca DELETE — a restrição
// dura do plano é que nenhuma batida gravada se perca.
private fun sanearUid(db: SupportSQLiteDatabase, table: String) {
    backfillUid(db, table)
    novoUid(
            db,
            table,
            ids(db, "SELECT id FROM $table WHERE id NOT IN (SELECT MIN(id) FROM $table GROUP BY uid)")
    )
}

private fun ids(db: SupportSQLiteDatabase, sql: String): List<Long> =
        db.query(sql).use { cursor ->
            val out = ArrayList<Long>()
            while (cursor.moveToNext()) out.add(cursor.getLong(0))
            out
        }

private fun novoUid(db: SupportSQLiteDatabase, table: String, ids: List<Long>) {
    for (id in ids) {
        db.execSQL(
                "UPDATE $table SET uid = ? WHERE id = ?",
                arrayOf(UUID.randomUUID().toString(), id)
        )
    }
}

@Database(
        entities =
                [Employee::class, Punch::class, Store::class, AppSetting::class, OutboxItem::class],
        version = 5,
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
                                                    MIGRATION_3_4,
                                                    MIGRATION_4_5
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
