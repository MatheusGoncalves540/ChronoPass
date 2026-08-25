package com.chronopass.app.data

import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType

// Pure business rules — no Android, unit-testable.
object PunchRules {
    /** Next punch type given the employee's last punch (null = none yet). */
    fun next(last: PunchType?): PunchType =
        if (last == PunchType.IN) PunchType.OUT else PunchType.IN

    /** Sums IN→OUT intervals in milliseconds. Punches must be time-ordered. */
    fun totalWorkedMs(punches: List<Punch>): Long {
        var total = 0L
        var pendingIn: Long? = null
        for (p in punches.sortedBy { it.timestamp }) {
            if (p.type == PunchType.IN) pendingIn = p.timestamp
            else pendingIn?.let { total += p.timestamp - it; pendingIn = null }
        }
        return total
    }
}
