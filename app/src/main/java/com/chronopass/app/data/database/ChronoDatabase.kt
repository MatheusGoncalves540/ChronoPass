package com.chronopass.app.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chronopass.app.data.dao.*
import com.chronopass.app.data.entities.*

class Converters {
    @TypeConverter fun toType(v: String) = PunchType.valueOf(v)
    @TypeConverter fun fromType(t: PunchType) = t.name
}

// v1 -> v2: soft-delete columns. Keeps existing data on device.
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE employee ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE punch ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [Employee::class, Punch::class, Store::class, AppSetting::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ChronoDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun punchDao(): PunchDao
    abstract fun storeDao(): StoreDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var instance: ChronoDatabase? = null
        fun get(context: Context): ChronoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, ChronoDatabase::class.java, "chronopass.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
        fun close() { instance?.close(); instance = null }
    }
}
