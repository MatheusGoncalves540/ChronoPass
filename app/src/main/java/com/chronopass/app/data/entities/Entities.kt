package com.chronopass.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class PunchType {
    IN,
    OUT
}

// uid único (v5): a unicidade deixou de ser convenção Kotlin — upsert vindo do Summus não cria
// mais duplicata silenciosa.
@Entity(tableName = "employee", indices = [Index(value = ["uid"], unique = true)])
data class Employee(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val uid: String? = UUID.randomUUID().toString(), // id externo p/ o Summus
        val name: String,
        val code: String = "",
        val photoPath: String? = null,
        val active: Boolean = true,
        val deleted: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
        val role: String? = null, // cargo — só o Summus preenche
        val origin: String = ORIGIN_LOCAL, // SUMMUS | LOCAL (quem é dono do cadastro)
)

const val ORIGIN_LOCAL = "LOCAL"
const val ORIGIN_SUMMUS = "SUMMUS"

@Entity(tableName = "punch", indices = [Index(value = ["uid"], unique = true)])
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
        // Revisão da correção aplicada pelo Summus; null = ponto nunca corrigido lá.
        val serverRevision: Int? = null,
)

@Entity(tableName = "store")
data class Store(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
        val uid: String? = null, // rh_stores.id quando a loja vem do Summus
        val managedBySummus: Boolean = false, // 1 = coordenada é somente-leitura na tela
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
        // Base do backoff (v5): createdAt não freava nada com mais de 5 min de idade.
        val lastAttemptAt: Long? = null,
)
