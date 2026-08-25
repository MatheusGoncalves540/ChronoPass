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

    fun formatDuration(ms: Long): String {
        val totalMin = ms / 60000
        return "%02d:%02d".format(totalMin / 60, totalMin % 60)
    }

    private fun cal(ts: Long) = Calendar.getInstance().apply { timeInMillis = ts }
}
