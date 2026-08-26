package com.chronopass.app.reports

import com.chronopass.app.data.PunchRules
import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import java.io.File

object CsvExport {
    fun write(file: File, punches: List<Punch>, names: Map<Long, String>) {
        // Almoço é uma métrica por dia; agrupa por funcionário+dia pra anexar
        // o mesmo valor em cada marcação daquele dia (útil pra filtrar/pivotar).
        val byEmployeeDay = punches.groupBy { it.employeeId to TimeUtil.startOfDay(it.timestamp) }
        val lunchByDay = byEmployeeDay.mapValues { (_, list) -> PunchRules.lunchMs(list) }
        val requiredByDay = byEmployeeDay.mapValues { (_, list) -> PunchRules.requiredLunchMs(PunchRules.totalWorkedMs(list)) }

        file.bufferedWriter().use { w ->
            w.appendLine("Funcionario,Data,Tipo,Horario,Latitude,Longitude,Precisao,Almoco,Almoco Insuficiente,Motivo da Alteracao")
            for (p in punches) {
                val name = names[p.employeeId] ?: "?"
                val tipo = if (p.type == PunchType.IN) "Entrada" else "Saida"
                val key = p.employeeId to TimeUtil.startOfDay(p.timestamp)
                val lunch = lunchByDay[key] ?: 0L
                val required = requiredByDay[key] ?: 0L
                w.appendLine(
                    listOf(
                        csv(name), TimeUtil.date(p.timestamp), tipo, TimeUtil.time(p.timestamp),
                        p.latitude?.toString() ?: "", p.longitude?.toString() ?: "",
                        p.accuracy?.let { "%.0f".format(it) } ?: "",
                        if (lunch > 0) TimeUtil.formatDuration(lunch) else "",
                        if (required > 0) (if (lunch < required) "Sim" else "Nao") else "",
                        csv(p.editReason ?: "")
                    ).joinToString(",")
                )
            }
        }
    }
    private fun csv(v: String) = if (v.contains(',') || v.contains('"'))
        "\"" + v.replace("\"", "\"\"") + "\"" else v
}
