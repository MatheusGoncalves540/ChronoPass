package com.chronopass.app.reports

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import java.io.File

object PdfExport {
    // ponytail: single mirror per employee/period; PdfDocument, no library.
    fun write(file: File, employeeName: String, period: String, punches: List<Punch>) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        val title = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val h = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val p = Paint().apply { textSize = 11f }

        var y = 50f
        c.drawText("CHRONOPASS", 40f, y, title); y += 22f
        c.drawText("Espelho de ponto", 40f, y, h); y += 24f
        c.drawText("Funcionario: $employeeName", 40f, y, p); y += 16f
        c.drawText("Periodo: $period", 40f, y, p); y += 24f
        c.drawText("Data        Tipo        Horario", 40f, y, h); y += 16f

        var total = 0L
        var pendingIn: Long? = null
        for (punch in punches) {
            val tipo = if (punch.type == PunchType.IN) "Entrada" else "Saida"
            c.drawText(
                "%-12s%-12s%s".format(TimeUtil.date(punch.timestamp), tipo, TimeUtil.time(punch.timestamp)),
                40f, y, p
            )
            y += 15f
            if (punch.type == PunchType.IN) pendingIn = punch.timestamp
            else pendingIn?.let { total += punch.timestamp - it; pendingIn = null }
            if (y > 800f) { doc.finishPage(page); return finish(doc, file) } // ponytail: single page cap
        }
        y += 10f
        c.drawText("Total de horas: ${TimeUtil.formatDuration(total)}", 40f, y, h)
        doc.finishPage(page)
        finish(doc, file)
    }
    private fun finish(doc: PdfDocument, file: File) {
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}
