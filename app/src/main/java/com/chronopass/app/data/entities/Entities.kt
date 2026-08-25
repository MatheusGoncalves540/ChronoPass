package com.chronopass.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PunchType { IN, OUT }

@Entity(tableName = "employee")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val active: Boolean = true,
    val deleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "punch")
data class Punch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val timestamp: Long,
    val type: PunchType,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // ponytail: simple edit history instead of full audit table
    val editedBy: String? = null,
    val editedAt: Long? = null,
    val editReason: String? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "store")
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String,
)
