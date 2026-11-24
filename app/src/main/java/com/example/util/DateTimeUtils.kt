package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val indonesiaLocale = Locale.forLanguageTag("id-ID")

    fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun currentTimeHHmm(): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }

    fun currentTimeClock(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }

    fun formattedIndonesianDate(): String {
        return SimpleDateFormat("EEEE, dd MMMM yyyy", indonesiaLocale).format(Date())
    }

    fun formatDateKeyReadable(dateKey: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(dateKey) ?: return dateKey
            SimpleDateFormat("EEE, dd MMM yyyy", indonesiaLocale).format(parsed)
        } catch (_: Exception) {
            dateKey
        }
    }

    fun isLateCheckIn(checkInTime: String, shiftStart: String): Boolean {
        val checkInMin = toMinutes(checkInTime) ?: return false
        val shiftMin = toMinutes(shiftStart) ?: return false
        return checkInMin > shiftMin + 10 // 10 minutes grace period
    }

    fun isValidTimeHHmm(time: String): Boolean {
        return Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(time.trim())
    }

    fun isValidDateKey(dateKey: String): Boolean {
        return Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dateKey.trim())
    }

    fun computeDurationLabel(checkInTime: String, checkOutTime: String?): String? {
        if (checkOutTime.isNullOrBlank()) return null
        val start = toMinutes(checkInTime) ?: return null
        val end = toMinutes(checkOutTime) ?: return null
        val diff = (end - start).coerceAtLeast(0)
        val hours = diff / 60
        val mins = diff % 60
        return "${hours}j ${mins}m"
    }

    private fun toMinutes(hhmm: String): Int? {
        val parts = hhmm.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
