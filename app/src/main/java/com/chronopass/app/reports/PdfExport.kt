package com.chronopass.app.reports

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.chronopass.app.data.PunchRules
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import java.io.File

object PdfExport {
    // Espelho de ponto: cabeçalho, tabela com bordas (Data/Entrada/Saída/Horas)
    // agregada por dia, total, e blocos de assinatura. PdfDocument, sem lib.
    // ponytail: uma página; ~31 dias cabem. Multi-página se um período grande exigir.
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val LEFT = 40f
    private const val RIGHT = 555f
    private val COLS = floatArrayOf(40f, 175f, 300f, 425f, 555f) // Data | Entrada | Saída | Horas

    fun write(file: File, employeeName: String, period: String, punches: List<Punch>, logo: Bitmap? = null) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas

        // Logo (configurada em build) no topo direito, altura fixa mantendo proporção.
        logo?.let { bmp ->
            val h = 46f
            val w = h * bmp.width / bmp.height
            val dst = RectF(RIGHT - w, 30f, RIGHT, 30f + h)
            c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dst, Paint().apply { isFilterBitmap = true })
        }

        val title = Paint().apply { textSize = 20f; isFakeBoldText = true; isAntiAlias = true }
        val sub = Paint().apply { textSize = 12f; isAntiAlias = true; color = Color.DKGRAY }
        val label = Paint().apply { textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        val cell = Paint().apply { textSize = 11f; isAntiAlias = true }
        val line = Paint().apply { color = Color.parseColor("#BBBBBB"); strokeWidth = 0.8f }
        val headerBg = Paint().apply { color = Color.parseColor("#EDE7F6") }

        var y = 55f
        c.drawText("CHRONOPASS", LEFT, y, title); y += 20f
        c.drawText("Espelho de ponto", LEFT, y, sub); y += 26f
        c.drawText("Funcionário: $employeeName", LEFT, y, cell); y += 16f
        c.drawText("Período: $period", LEFT, y, cell); y += 24f

        // --- tabela ---
        val rowH = 22f
        val tableTop = y
        // cabeçalho
        c.drawRect(LEFT, y, RIGHT, y + rowH, headerBg)
        drawRow(c, y, arrayOf("Data", "Entrada", "Saída", "Horas"), label)
        y += rowH

        val byDay = punches.groupBy { TimeUtil.startOfDay(it.timestamp) }.toSortedMap()
        var totalMs = 0L
        for ((day, dayPunches) in byDay) {
            val ins = dayPunches.filter { it.type == PunchType.IN }.minByOrNull { it.timestamp }
            val outs = dayPunches.filter { it.type == PunchType.OUT }.maxByOrNull { it.timestamp }
            val worked = PunchRules.totalWorkedMs(dayPunches)
            totalMs += worked
            drawRow(c, y, arrayOf(
                TimeUtil.date(day),
                ins?.let { TimeUtil.hm(it.timestamp) } ?: "—",
                outs?.let { TimeUtil.hm(it.timestamp) } ?: "—",
                TimeUtil.formatDuration(worked)
            ), cell)
            y += rowH
            if (y > 690f) break // ponytail: single-page cap
        }
        if (byDay.isEmpty()) {
            c.drawText("Nenhuma marcação no período.", LEFT + 6f, y + 15f, cell); y += rowH
        }

        // total
        c.drawRect(LEFT, y, RIGHT, y + rowH, headerBg)
        drawRow(c, y, arrayOf("", "", "Total", TimeUtil.formatDuration(totalMs)), label)
        y += rowH

        // grade da tabela (bordas)
        val tableBottom = y
        var gy = tableTop
        while (gy <= tableBottom + 0.1f) { c.drawLine(LEFT, gy, RIGHT, gy, line); gy += rowH }
        for (x in COLS) c.drawLine(x, tableTop, x, tableBottom, line)

        // --- assinaturas ---
        val sigY = 770f
        val midGap = 40f
        val colW = (RIGHT - LEFT - midGap) / 2
        val sig = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }
        c.drawLine(LEFT, sigY, LEFT + colW, sigY, sig)
        c.drawLine(RIGHT - colW, sigY, RIGHT, sigY, sig)
        val sigLabel = Paint().apply { textSize = 10f; isAntiAlias = true; color = Color.DKGRAY; textAlign = Paint.Align.CENTER }
        c.drawText("Assinatura do funcionário", LEFT + colW / 2, sigY + 14f, sigLabel)
        c.drawText("Assinatura do gerente", RIGHT - colW / 2, sigY + 14f, sigLabel)

        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private fun drawRow(c: android.graphics.Canvas, top: Float, cells: Array<String>, paint: Paint) {
        val baseline = top + 15f
        for (i in cells.indices) {
            c.drawText(cells[i], COLS[i] + 6f, baseline, paint)
        }
    }
}
