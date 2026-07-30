package com.example.meditatenow

import org.junit.Test
import org.junit.Assert.*

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
}