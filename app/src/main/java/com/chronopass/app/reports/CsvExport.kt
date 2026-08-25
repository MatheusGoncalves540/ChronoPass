package com.chronopass.app.reports

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import java.io.File

object CsvExport {
    fun write(file: File, punches: List<Punch>, names: Map<Long, String>) {
        file.bufferedWriter().use { w ->
            w.appendLine("Funcionario,Data,Tipo,Horario,Latitude,Longitude,Precisao")
            for (p in punches) {
                val name = names[p.employeeId] ?: "?"
                val tipo = if (p.type == PunchType.IN) "Entrada" else "Saida"
                w.appendLine(
                    listOf(
                        csv(name), TimeUtil.date(p.timestamp), tipo, TimeUtil.time(p.timestamp),
                        p.latitude?.toString() ?: "", p.longitude?.toString() ?: "",
                        p.accuracy?.let { "%.0f".format(it) } ?: ""
                    ).joinToString(",")
                )
            }
        }
    }
    private fun csv(v: String) = if (v.contains(',') || v.contains('"'))
        "\"" + v.replace("\"", "\"\"") + "\"" else v
}
