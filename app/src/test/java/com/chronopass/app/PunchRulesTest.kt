package com.chronopass.app

import com.chronopass.app.data.PunchRules
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import org.junit.Assert.assertEquals
import org.junit.Test

class PunchRulesTest {
    @Test fun noPunchYet_isEntrada() = assertEquals(PunchType.IN, PunchRules.next(null))
    @Test fun afterEntrada_isSaida() = assertEquals(PunchType.OUT, PunchRules.next(PunchType.IN))
    @Test fun afterSaida_isEntrada() = assertEquals(PunchType.IN, PunchRules.next(PunchType.OUT))

    @Test fun totalWorked_sumsIntervals() {
        val h = 3_600_000L
        val p = listOf(
            Punch(employeeId = 1, timestamp = 0, type = PunchType.IN),
            Punch(employeeId = 1, timestamp = 8 * h, type = PunchType.OUT),
            Punch(employeeId = 1, timestamp = 9 * h, type = PunchType.IN),
            Punch(employeeId = 1, timestamp = 10 * h, type = PunchType.OUT),
        )
        assertEquals(9 * h, PunchRules.totalWorkedMs(p))
    }

    @Test fun lunchMs_isGapBetweenPairs() {
        val h = 3_600_000L
        val p = listOf(
            Punch(employeeId = 1, timestamp = 0, type = PunchType.IN),
            Punch(employeeId = 1, timestamp = 4 * h, type = PunchType.OUT),
            Punch(employeeId = 1, timestamp = 5 * h, type = PunchType.IN),
            Punch(employeeId = 1, timestamp = 9 * h, type = PunchType.OUT),
        )
        assertEquals(h, PunchRules.lunchMs(p))
    }

    @Test fun lunchMs_zeroWithoutRegisteredBreak() {
        val h = 3_600_000L
        val p = listOf(
            Punch(employeeId = 1, timestamp = 0, type = PunchType.IN),
            Punch(employeeId = 1, timestamp = 8 * h, type = PunchType.OUT),
        )
        assertEquals(0L, PunchRules.lunchMs(p))
    }

    @Test fun lunchMs_zeroWhenDayStillOpen() {
        val p = listOf(Punch(employeeId = 1, timestamp = 0, type = PunchType.IN))
        assertEquals(0L, PunchRules.lunchMs(p))
    }

    @Test fun handlesDuplicateAdjacentPunch_withoutLosingTheGap() {
        // Dia começando com Saída solta + Entrada duplicada (edição manual bagunçada):
        // 12:09 Saída, 13:09 Entrada, 14:08 Entrada, 22:08 Saída.
        val p = listOf(
            Punch(employeeId = 1, timestamp = h(12, 9), type = PunchType.OUT),
            Punch(employeeId = 1, timestamp = h(13, 9), type = PunchType.IN),
            Punch(employeeId = 1, timestamp = h(14, 8), type = PunchType.IN),
            Punch(employeeId = 1, timestamp = h(22, 8), type = PunchType.OUT),
        )
        assertEquals(60 * 60_000L, PunchRules.lunchMs(p)) // 12:09 -> 13:09
        assertEquals(h(22, 8) - h(13, 9), PunchRules.totalWorkedMs(p)) // 13:09 -> 22:08
    }

    private fun h(hour: Int, min: Int) = (hour * 3_600_000L) + (min * 60_000L)

    @Test fun requiredLunch_thresholds() {
        val h = 3_600_000L
        assertEquals(0L, PunchRules.requiredLunchMs(4 * h))
        assertEquals(30 * 60_000L, PunchRules.requiredLunchMs(5 * h))
        assertEquals(60 * 60_000L, PunchRules.requiredLunchMs(7 * h))
    }
}
