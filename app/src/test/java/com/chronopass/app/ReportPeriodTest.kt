package com.chronopass.app

import com.chronopass.app.reports.ReportPeriod
import com.chronopass.app.reports.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReportPeriodTest {
    // 15/03/2024, 14:30 local
    private val now = Calendar.getInstance().apply {
        set(2024, Calendar.MARCH, 15, 14, 30, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun day(ts: Long) = TimeUtil.date(ts)

    @Test fun thisMonth_startsOnDay1_endsToday() {
        val (from, to) = ReportPeriod.THIS_MONTH.range(now)
        assertEquals("01/03/2024", day(from))
        assertEquals("15/03/2024", day(to))
    }

    @Test fun lastMonth_coversWholeMonth() {
        val (from, to) = ReportPeriod.LAST_MONTH.range(now)
        assertEquals("01/02/2024", day(from))
        assertEquals("29/02/2024", day(to)) // ano bissexto
    }

    @Test fun last7_includesToday() {
        val (from, to) = ReportPeriod.LAST_7.range(now)
        assertEquals("09/03/2024", day(from))
        assertEquals("15/03/2024", day(to))
    }

    @Test fun last30_includesToday() {
        assertEquals("15/02/2024", day(ReportPeriod.LAST_30.range(now).first))
    }

    @Test fun rangesAreOrderedAndCoverTheDay() {
        for (p in ReportPeriod.entries) {
            val (from, to) = p.range(now)
            assertTrue(p.name, from < to)
            assertEquals(p.name, 0, from % 1000)
        }
    }
}
