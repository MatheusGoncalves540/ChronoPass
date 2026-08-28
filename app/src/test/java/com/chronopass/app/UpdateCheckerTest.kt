package com.chronopass.app

import com.chronopass.app.update.UpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun isNewer_detectsHigherMinor() = assertTrue(UpdateChecker.isNewer("2.1.0", "2.0.0"))
    @Test fun isNewer_same_isNotNewer() = assertFalse(UpdateChecker.isNewer("2.0.0", "2.0.0"))
    @Test fun isNewer_older_isNotNewer() = assertFalse(UpdateChecker.isNewer("1.9.9", "2.0.0"))
    @Test fun isNewer_extraSegment_wins() = assertTrue(UpdateChecker.isNewer("2.0.0.1", "2.0.0"))
    @Test fun isNewer_missingSegment_isEqual() = assertFalse(UpdateChecker.isNewer("2.0", "2.0.0"))
}
