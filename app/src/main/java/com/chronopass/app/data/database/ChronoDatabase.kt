package com.chronopass.app.data.database

import android.content.Context
import androidx.room.*
import com.chronopass.app.data.dao.*
import com.chronopass.app.data.entities.*

class Converters {
    @TypeConverter fun toType(v: String) = PunchType.valueOf(v)
    @TypeConverter fun fromType(t: PunchType) = t.name
}

@Database(
    entities = [Employee::class, Punch::class, Store::class, AppSetting::class],
    version = 1,
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
            ).build().also { instance = it }
        }
        fun close() { instance?.close(); instance = null }
    }
}
