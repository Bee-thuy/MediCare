package com.example.medicare

import com.example.medicare.model.ShiftTimeNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftTimeNormalizerTest {
    @Test fun legacyAfternoonClockBecomesTwentyFourHourTime() {
        assertEquals("13:30" to "17:30", ShiftTimeNormalizer.normalize("Ca chiều", "01:30", "05:30"))
        assertEquals("13:30" to "17:30", ShiftTimeNormalizer.normalize("Ca chieu", "01:30", "05:30"))
    }

    @Test fun validAfternoonAndMorningStayAsStored() {
        assertEquals("13:30" to "17:30", ShiftTimeNormalizer.normalize("Ca chiều", "13:30", "17:30"))
        assertEquals("08:00" to "12:00", ShiftTimeNormalizer.normalize("Ca sáng", "08:00", "12:00"))
    }
}
