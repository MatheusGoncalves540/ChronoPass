package com.chronopass.app.data

import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType

// Pure business rules — no Android, unit-testable.
object PunchRules {
    /** Next punch type given the employee's last punch (null = none yet). */
    fun next(last: PunchType?): PunchType =
        if (last == PunchType.IN) PunchType.OUT else PunchType.IN

    /** Sums IN→OUT intervals in milliseconds. Punches must be time-ordered. */
    fun totalWorkedMs(punches: List<Punch>): Long {
        var total = 0L
        var pendingIn: Long? = null
        for (p in punches.sortedBy { it.timestamp }) {
            if (p.type == PunchType.IN) pendingIn = p.timestamp
            else pendingIn?.let { total += p.timestamp - it; pendingIn = null }
        }
        return total
    }

    /**
     * Soma de todos os intervalos (almoço + pausas) dentro de um único dia:
     * o tempo entre a primeira Entrada e a última Saída que não é trabalhado.
     * Exige que o dia comece com Entrada e termine com Saída (dia "fechado");
     * caso contrário não há como distinguir pausa de jornada ainda em curso.
     */
    fun lunchMs(dayPunches: List<Punch>): Long {
        val sorted = dayPunches.sortedBy { it.timestamp }
        if (sorted.size < 2 || sorted.first().type != PunchType.IN || sorted.last().type != PunchType.OUT) return 0L
        val span = sorted.last().timestamp - sorted.first().timestamp
        return (span - totalWorkedMs(sorted)).coerceAtLeast(0L)
    }

    /** Intervalo mínimo legal (CLT): 1h para jornada >6h, 30min para 4-6h, nenhum abaixo disso. */
    fun requiredLunchMs(workedMs: Long): Long {
        val h = 3_600_000L
        return when {
            workedMs > 6 * h -> 60 * 60_000L
            workedMs > 4 * h -> 30 * 60_000L
            else -> 0L
        }
    }
}
