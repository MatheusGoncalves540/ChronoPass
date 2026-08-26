package com.chronopass.app.data

import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType

// Pure business rules — no Android, unit-testable.
object PunchRules {
    /** Next punch type given the employee's last punch (null = none yet). */
    fun next(last: PunchType?): PunchType =
        if (last == PunchType.IN) PunchType.OUT else PunchType.IN

    /**
     * Ordena e reduz marcações consecutivas do mesmo tipo à primeira da sequência.
     * Correções manuais podem deixar duas Entradas seguidas sem Saída no meio (ou
     * vice-versa); sem isso o par seguinte casaria com a marcação errada e o
     * intervalo entre elas desaparecia dos cálculos sem gerar nenhum aviso.
     */
    private fun normalized(punches: List<Punch>): List<Punch> {
        val sorted = punches.sortedBy { it.timestamp }
        val result = mutableListOf<Punch>()
        for (p in sorted) if (result.isEmpty() || result.last().type != p.type) result += p
        return result
    }

    /** Sums IN→OUT intervals in milliseconds. Marcação sem par (Entrada pendente
     * no fim, ou Saída solta no início) é ignorada, não descarta o resto. */
    fun totalWorkedMs(punches: List<Punch>): Long {
        val n = normalized(punches)
        var total = 0L
        for (i in 0 until n.size - 1) {
            if (n[i].type == PunchType.IN && n[i + 1].type == PunchType.OUT) total += n[i + 1].timestamp - n[i].timestamp
        }
        return total
    }

    /** Soma dos intervalos Saída→Entrada (almoço/pausas) dentro de um dia. */
    fun lunchMs(dayPunches: List<Punch>): Long {
        val n = normalized(dayPunches)
        var total = 0L
        for (i in 0 until n.size - 1) {
            if (n[i].type == PunchType.OUT && n[i + 1].type == PunchType.IN) total += n[i + 1].timestamp - n[i].timestamp
        }
        return total
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
