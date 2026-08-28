package com.chronopass.app.reports

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.chronopass.app.data.PunchRules
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import java.io.File

object PdfExport {
    // Espelho de ponto: cabeçalho (repetido em cada página), tabela com bordas
    // (Data/Entrada/Saída/Almoço/Horas) agregada por dia, total, observações de
    // alterações (motivo) e blocos de assinatura. PdfDocument, sem lib.
    // Pagina automaticamente quando o período não cabe em uma página; numera
    // "Página X de Y" no rodapé só quando o relatório passa de 2 páginas.
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val LEFT = 40f
    private const val RIGHT = 555f
    private const val ROW_H = 22f
    private const val TABLE_TOP = 141f
    private const val BODY_BOTTOM = 780f // margem de segurança p/ rodapé/assinatura
    private const val LUNCH_COL = 3 // índice da coluna Almoço em COLS/cells
    private val COLS =
            floatArrayOf(
                    40f,
                    140f,
                    235f,
                    330f,
                    430f,
                    555f
            ) // Data | Entrada | Saída | Almoço | Horas
    private const val PHOTO_W = 114f
    private const val PHOTO_H = 103f
    private const val LOGO_H = 56f
    private const val LOGO_MAX_W = 210f

    private class Row(
            val cells: Array<String>?,
            val bold: Boolean = false,
            val msg: String? = null,
            val warnLunch: Boolean = false
    )

    fun write(
            file: File,
            employeeName: String,
            period: String,
            punches: List<Punch>,
            logo: Bitmap? = null,
            photo: Bitmap? = null
    ) {
        val label =
                Paint().apply {
                    textSize = 11f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
        val cell =
                Paint().apply {
                    textSize = 11f
                    isAntiAlias = true
                }
        val notePaint =
                Paint().apply {
                    textSize = 9.5f
                    isAntiAlias = true
                    color = Color.DKGRAY
                }

        // --- linhas da tabela (por dia + total) ---
        val byDay = punches.groupBy { TimeUtil.startOfDay(it.timestamp) }.toSortedMap()
        val rows = mutableListOf<Row>()
        var totalMs = 0L
        var totalLunchMs = 0L
        var anyLunchWarn = false
        if (byDay.isEmpty()) {
            rows += Row(null, msg = "Nenhuma marcação no período.")
        } else {
            for ((day, dayPunches) in byDay) {
                val ins = dayPunches.filter { it.type == PunchType.IN }.minByOrNull { it.timestamp }
                val outs =
                        dayPunches.filter { it.type == PunchType.OUT }.maxByOrNull { it.timestamp }
                val worked = PunchRules.totalWorkedMs(dayPunches)
                val lunch = PunchRules.lunchMs(dayPunches)
                val required = PunchRules.requiredLunchMs(worked)
                val warn = required > 0 && lunch < required
                totalMs += worked
                totalLunchMs += lunch
                if (warn) anyLunchWarn = true
                rows +=
                        Row(
                                arrayOf(
                                        TimeUtil.date(day),
                                        ins?.let { TimeUtil.hm(it.timestamp) } ?: "—",
                                        outs?.let { TimeUtil.hm(it.timestamp) } ?: "—",
                                        (if (lunch > 0) TimeUtil.formatDuration(lunch) else "—") +
                                                (if (warn) "*" else ""),
                                        TimeUtil.formatDuration(worked)
                                ),
                                warnLunch = warn
                        )
            }
        }
        rows +=
                Row(
                        arrayOf(
                                "",
                                "",
                                "Total",
                                TimeUtil.formatDuration(totalLunchMs),
                                TimeUtil.formatDuration(totalMs)
                        ),
                        bold = true
                )

        // --- observações: motivo das marcações alteradas, com quebra de linha ---
        val noteLines =
                punches.filter { it.editReason != null }.sortedBy { it.timestamp }.flatMap { p ->
                    val tipo = if (p.type == PunchType.IN) "Entrada" else "Saída"
                    val text =
                            "${TimeUtil.date(p.timestamp)} ${TimeUtil.hm(p.timestamp)} ($tipo) — " +
                                    "Alterado por ${p.editedBy ?: "?"}: ${p.editReason}"
                    wrapText(text, notePaint, RIGHT - LEFT)
                }

        val legendLine =
                if (anyLunchWarn) "* Intervalo de almoço abaixo do mínimo legal (CLT)." else null

        // --- paginação da tabela ---
        val rowsPerPage = ((BODY_BOTTOM - TABLE_TOP - ROW_H) / ROW_H).toInt().coerceAtLeast(1)
        val tablePages = rows.chunked(rowsPerPage)

        val legendHeight = if (legendLine == null) 0f else 16f
        val notesHeight = if (noteLines.isEmpty()) 0f else 20f + noteLines.size * 13f
        val signatureHeight = 70f
        val lastPageUsedBottom = TABLE_TOP + ROW_H + tablePages.last().size * ROW_H
        val trailingFitsLastPage =
                BODY_BOTTOM - lastPageUsedBottom >= legendHeight + notesHeight + signatureHeight
        val totalPages = tablePages.size + if (trailingFitsLastPage) 0 else 1

        val doc = PdfDocument()
        for (pageIndex in 0 until totalPages) {
            val page =
                    doc.startPage(
                            PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageIndex + 1).create()
                    )
            val c = page.canvas
            drawHeader(c, employeeName, period, logo, photo)

            val isTablePage = pageIndex < tablePages.size
            var y = TABLE_TOP
            if (isTablePage) {
                y = drawTablePage(c, tablePages[pageIndex], label, cell)
            }

            val isLastPage = pageIndex == totalPages - 1
            if (isLastPage) {
                if (legendLine != null) {
                    y += 14f
                    c.drawText(legendLine, LEFT, y, notePaint)
                    y += 2f
                }
                if (noteLines.isNotEmpty()) y = drawNotes(c, y, noteLines, notePaint)
                drawSignatures(c)
            }

            if (totalPages > 2) drawPageNumber(c, pageIndex + 1, totalPages)

            doc.finishPage(page)
        }
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private fun drawHeader(
            c: android.graphics.Canvas,
            employeeName: String,
            period: String,
            logo: Bitmap?,
            photo: Bitmap?
    ) {
        // Logo grande centralizada no cabeçalho, entre o título (esquerda) e a foto (direita).
        logo?.let { bmp ->
            var h = LOGO_H
            var w = h * bmp.width / bmp.height
            if (w > LOGO_MAX_W) {
                w = LOGO_MAX_W
                h = w * bmp.height / bmp.width
            }
            val cx = (LEFT + RIGHT) / 2f
            val dst = RectF(cx - w / 2f, 28f, cx + w / 2f, 28f + h)
            c.drawBitmap(
                    bmp,
                    Rect(0, 0, bmp.width, bmp.height),
                    dst,
                    Paint().apply { isFilterBitmap = true }
            )
        }
        drawEmployeePhoto(c, employeeName, photo)
        val title =
                Paint().apply {
                    textSize = 20f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
        val sub =
                Paint().apply {
                    textSize = 12f
                    isAntiAlias = true
                    color = Color.DKGRAY
                }
        val cell =
                Paint().apply {
                    textSize = 11f
                    isAntiAlias = true
                }

        var y = 55f
        c.drawText("CHRONOPASS", LEFT, y, title)
        y += 20f
        c.drawText("Espelho de ponto", LEFT, y, sub)
        y += 26f
        // Funcionário/Período: área de texto limitada para nunca invadir a moldura da foto.
        c.save()
        c.clipRect(LEFT, 85f, RIGHT - PHOTO_W - 10f, 135f)
        c.drawText("Funcionário: $employeeName", LEFT, y, cell)
        y += 16f
        c.drawText("Período: $period", LEFT, y, cell)
        c.restore()
    }

    // Foto do colaborador: moldura arredondada grande no topo direito. Sem foto, um
    // placeholder com as iniciais mantém o cabeçalho estruturado. Center-crop preserva
    // a proporção da imagem (não estica na moldura).
    private fun drawEmployeePhoto(
            c: android.graphics.Canvas,
            employeeName: String,
            photo: Bitmap?
    ) {
        val dst = RectF(RIGHT - PHOTO_W, 30f, RIGHT, 30f + PHOTO_H)
        val frame =
                Paint().apply {
                    color = Color.parseColor("#EDE7F6")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f
                    isAntiAlias = true
                }
        val path = Path().apply { addRoundRect(dst, 8f, 8f, Path.Direction.CW) }
        c.save()
        c.clipPath(path)
        c.drawRect(dst, Paint().apply { color = Color.WHITE })
        if (photo != null) {
            // Center-crop: recorta a foto pra preencher a moldura sem distorcer.
            val scale = maxOf(dst.width() / photo.width, dst.height() / photo.height)
            val srcW = (dst.width() / scale).toInt().coerceAtMost(photo.width)
            val srcH = (dst.height() / scale).toInt().coerceAtMost(photo.height)
            val srcLeft = (photo.width - srcW) / 2
            val srcTop = (photo.height - srcH) / 2
            c.drawBitmap(
                    photo,
                    Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH),
                    dst,
                    Paint().apply { isFilterBitmap = true }
            )
        } else {
            val p =
                    Paint().apply {
                        textSize = 26f
                        isFakeBoldText = true
                        isAntiAlias = true
                        color = Color.GRAY
                        textAlign = Paint.Align.CENTER
                    }
            c.drawText(initials(employeeName), dst.centerX(), dst.centerY() + 6f, p)
        }
        c.restore()
        c.drawRoundRect(dst, 8f, 8f, frame)
    }

    private fun initials(name: String): String =
            name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") {
                it.first().uppercaseChar().toString()
            }

    /** Desenha o cabeçalho de colunas + linhas desta página; retorna o y após a tabela. */
    private fun drawTablePage(
            c: android.graphics.Canvas,
            pageRows: List<Row>,
            label: Paint,
            cell: Paint
    ): Float {
        val headerBg = Paint().apply { color = Color.parseColor("#EDE7F6") }
        val line =
                Paint().apply {
                    color = Color.parseColor("#BBBBBB")
                    strokeWidth = 0.8f
                }

        val warnBg = Paint().apply { color = Color.parseColor("#FBE0E0") }
        var y = TABLE_TOP
        val tableTop = y
        c.drawRect(LEFT, y, RIGHT, y + ROW_H, headerBg)
        drawRow(c, y, arrayOf("Data", "Entrada", "Saída", "Almoço", "Horas"), label)
        y += ROW_H

        for (row in pageRows) {
            if (row.msg != null) {
                c.drawText(row.msg, LEFT + 6f, y + 15f, cell)
            } else {
                if (row.bold) c.drawRect(LEFT, y, RIGHT, y + ROW_H, headerBg)
                else if (row.warnLunch)
                        c.drawRect(COLS[LUNCH_COL], y, COLS[LUNCH_COL + 1], y + ROW_H, warnBg)
                drawRow(c, y, row.cells!!, if (row.bold) label else cell)
            }
            y += ROW_H
        }

        val tableBottom = y
        var gy = tableTop
        while (gy <= tableBottom + 0.1f) {
            c.drawLine(LEFT, gy, RIGHT, gy, line)
            gy += ROW_H
        }
        for (x in COLS) c.drawLine(x, tableTop, x, tableBottom, line)
        return y
    }

    private fun drawNotes(
            c: android.graphics.Canvas,
            top: Float,
            lines: List<String>,
            paint: Paint
    ): Float {
        var y = top + 20f
        val titlePaint =
                Paint().apply {
                    textSize = 10.5f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
        c.drawText("Alterações", LEFT, y, titlePaint)
        y += 15f
        for (l in lines) {
            c.drawText(l, LEFT, y, paint)
            y += 13f
        }
        return y
    }

    private fun drawSignatures(c: android.graphics.Canvas) {
        val sigY = 770f
        val midGap = 40f
        val colW = (RIGHT - LEFT - midGap) / 2
        val sig =
                Paint().apply {
                    color = Color.DKGRAY
                    strokeWidth = 1f
                }
        c.drawLine(LEFT, sigY, LEFT + colW, sigY, sig)
        c.drawLine(RIGHT - colW, sigY, RIGHT, sigY, sig)
        val sigLabel =
                Paint().apply {
                    textSize = 10f
                    isAntiAlias = true
                    color = Color.DKGRAY
                    textAlign = Paint.Align.CENTER
                }
        c.drawText("Assinatura do funcionário", LEFT + colW / 2, sigY + 14f, sigLabel)
        c.drawText("Assinatura do gerente", RIGHT - colW / 2, sigY + 14f, sigLabel)
    }

    private fun drawPageNumber(c: android.graphics.Canvas, current: Int, total: Int) {
        val paint =
                Paint().apply {
                    textSize = 9f
                    isAntiAlias = true
                    color = Color.DKGRAY
                    textAlign = Paint.Align.CENTER
                }
        c.drawText("Página $current de $total", (LEFT + RIGHT) / 2, 825f, paint)
    }

    private fun drawRow(
            c: android.graphics.Canvas,
            top: Float,
            cells: Array<String>,
            paint: Paint
    ) {
        val baseline = top + 15f
        for (i in cells.indices) {
            c.drawText(cells[i], COLS[i] + 6f, baseline, paint)
        }
    }

    /** Quebra de linha simples (greedy) para caber texto dentro de maxWidth. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder(w)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }
}
