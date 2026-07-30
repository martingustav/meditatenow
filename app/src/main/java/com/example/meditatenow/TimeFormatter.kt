package com.example.meditatenow

import java.util.Locale

/**
 * Formats seconds into a string of minutes and hours
 */
fun formatTime(totalSeconds: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/**
 * Converts minutes and seconds into total seconds
 */
fun toTotalSeconds(minutes: Int, seconds: Int): Int {
    return minutes * 60 + seconds
}