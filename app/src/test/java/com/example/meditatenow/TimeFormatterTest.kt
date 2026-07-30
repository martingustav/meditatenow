package com.example.meditatenow

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for pure time-conversion functions in TimeFormatter.kt.
 */
class TimeFormatterTest {
    @Test
    fun formatTime_formatCheck() {
        assertEquals("10:00", formatTime(600))
    }

    @Test
    fun formatTime_formatCheck_singleDigits() {
        assertEquals("01:05", formatTime(65))
    }

    @Test
    fun formatTime_formatCheck_zero() {
        assertEquals("00:00", formatTime(0))
    }

    @Test
    fun formatTime_formatCheck_midRange() {
        assertEquals("02:05", formatTime(125))
    }

    @Test
    fun toTotalSeconds_isCorrect() {
        assertEquals(330, toTotalSeconds(5, 30))
    }

    @Test
    fun toTotalSeconds_isCorrect_zeroMinutes() {
        assertEquals(9, toTotalSeconds(0, 9))
    }

    @Test
    fun toTotalSeconds_isCorrect_zeroSeconds() {
        assertEquals(120, toTotalSeconds(2, 0))
    }

    @Test
    fun toTotalSeconds_isCorrect_zero() {
        assertEquals(0, toTotalSeconds(0, 0))
    }
}