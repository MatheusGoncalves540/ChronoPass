package com.chronopass.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PunchType {
    IN,
    OUT
}

@Entity(tableName = "employee")
data class Employee(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val uid: String? = UUID.randomUUID().toString(), // id externo p/ o Summus
        val name: String,
        val code: String = "",
        val photoPath: String? = null,
        val active: Boolean = true,
        val deleted: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "punch")
data class Punch(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val uid: String? = UUID.randomUUID().toString(), // id externo p/ o Summus
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

// Fila de envio ao Summus (SUMUS-INTEGRACAO.md §7). Só o schema — enfileirar/drenar é fase
// posterior.
@Entity(tableName = "sync_outbox")
data class OutboxItem(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val tipo: String, // EMPLOYEE | PUNCH | PHOTO
        val refUid: String? = null, // uid da entidade (foto de employee: "employee.<uid>")
        val payload: String, // JSON do estado atual da entidade
        val status: String = "PENDING", // PENDING | FAILED | DONE
        val tentativas: Int = 0,
        val ultimoErro: String? = null,
        val createdAt: Long = System.currentTimeMillis(),
)
