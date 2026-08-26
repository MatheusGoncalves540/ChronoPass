package com.chronopass.app.reports

import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {
    private val date = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val time = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR"))
    private val hm = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

    fun date(ts: Long): String = date.format(Date(ts))
    fun time(ts: Long): String = time.format(Date(ts))
    fun hm(ts: Long): String = hm.format(Date(ts))

    fun startOfDay(ts: Long): Long = cal(ts).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfDay(ts: Long): Long = startOfDay(ts) + 24L * 60 * 60 * 1000 - 1

    fun addDays(ts: Long, n: Int): Long = cal(ts).apply { add(Calendar.DAY_OF_MONTH, n) }.timeInMillis
    fun addMonths(ts: Long, n: Int): Long = cal(ts).apply { add(Calendar.MONTH, n) }.timeInMillis

    fun startOfMonth(ts: Long): Long =
        startOfDay(cal(ts).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis)

    fun endOfMonth(ts: Long): Long =
        endOfDay(cal(ts).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }.timeInMillis)

    // O DateRangePicker do Material3 devolve meia-noite UTC; converte para o
    // mesmo dia no fuso local antes de virar intervalo.
    fun fromPickerUtc(utc: Long): Long = utc - TimeZone.getDefault().getOffset(utc)

    // yyyy-MM-dd para nome de arquivo (ordenavel, sem barra).
    fun fileDate(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))

    fun formatDuration(ms: Long): String {
        val totalMin = ms / 60000
        return "%02d:%02d".format(totalMin / 60, totalMin % 60)
    }

    private fun cal(ts: Long) = Calendar.getInstance().apply { timeInMillis = ts }
}
