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

    @Test fun requiredLunch_thresholds() {
        val h = 3_600_000L
        assertEquals(0L, PunchRules.requiredLunchMs(4 * h))
        assertEquals(30 * 60_000L, PunchRules.requiredLunchMs(5 * h))
        assertEquals(60 * 60_000L, PunchRules.requiredLunchMs(7 * h))
    }
}
