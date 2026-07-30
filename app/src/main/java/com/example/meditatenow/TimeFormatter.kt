package com.example.meditatenow

import java.util.Locale

/**
 * Formats seconds into a string of minutes and hours
 */
fun formatTime(totalSeconds: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}