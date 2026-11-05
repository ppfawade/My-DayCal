package com.example.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val monthNamesShort = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    private val monthNamesFull = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private val dayOfWeekShort = arrayOf(
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    )
    private val dayOfWeekFull = arrayOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    fun todayDateKey(): String {
        val cal = Calendar.getInstance()
        return formatCalendar(cal)
    }

    fun currentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun formatCalendar(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun parseDateKey(dateKey: String): Calendar {
        val cal = Calendar.getInstance()
        val parts = dateKey.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
            val m = (parts[1].toIntOrNull() ?: 1) - 1
            val d = parts[2].toIntOrNull() ?: 1
            cal.set(y, m, d, 12, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return cal
    }

    fun dateKeyToUtcMillis(dateKey: String): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val parts = dateKey.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: currentYear()
            val m = (parts[1].toIntOrNull() ?: 1) - 1
            val d = parts[2].toIntOrNull() ?: 1
            cal.set(y, m, d, 12, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun utcMillisToDateKey(utcMillis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = utcMillis
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun addDays(dateKey: String, offsetDays: Int): String {
        val cal = parseDateKey(dateKey)
        cal.add(Calendar.DAY_OF_MONTH, offsetDays)
        return formatCalendar(cal)
    }

    /**
     * Returns the ISO-8601 Calendar Week (CW) number (1..53) for the given dateKey.
     */
    fun getCalendarWeekNumber(dateKey: String): Int {
        val cal = parseDateKey(dateKey)
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        return cal.get(Calendar.WEEK_OF_YEAR)
    }

    /**
     * Returns Monday through Friday (Mon..Fri) of the ISO week containing [dateKey].
     */
    fun getWorkWeekDays(dateKey: String): List<DayChipInfo> {
        val today = todayDateKey()
        val cal = parseDateKey(dateKey)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Compute offset back to Monday of this ISO week (Sunday belongs to end of Mon..Sun week)
        val offsetToMonday = if (dow == Calendar.SUNDAY) -6 else (Calendar.MONDAY - dow)
        cal.add(Calendar.DAY_OF_MONTH, offsetToMonday)
        val mondayKey = formatCalendar(cal)

        return (0..4).map { dayOffset ->
            val key = addDays(mondayKey, dayOffset)
            val dCal = parseDateKey(key)
            val dDow = dCal.get(Calendar.DAY_OF_WEEK) - 1
            DayChipInfo(
                dateKey = key,
                dayOfMonth = dCal.get(Calendar.DAY_OF_MONTH),
                dayOfWeekShort = dayOfWeekShort[dDow.coerceIn(0, 6)],
                monthShort = monthNamesShort[dCal.get(Calendar.MONTH).coerceIn(0, 11)],
                year = dCal.get(Calendar.YEAR),
                isToday = key == today
            )
        }
    }

    fun getSurroundingDays(centerDateKey: String, radius: Int = 3): List<DayChipInfo> {
        val today = todayDateKey()
        return (-radius..radius).map { offset ->
            val key = addDays(centerDateKey, offset)
            val cal = parseDateKey(key)
            val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
            DayChipInfo(
                dateKey = key,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                dayOfWeekShort = dayOfWeekShort[dow.coerceIn(0, 6)],
                monthShort = monthNamesShort[cal.get(Calendar.MONTH).coerceIn(0, 11)],
                year = cal.get(Calendar.YEAR),
                isToday = key == today
            )
        }
    }

    /**
     * Formats date as "MMM XX" (e.g., "Nov 05") for the top center header.
     */
    fun formatShortMonthDay(dateKey: String): String {
        val cal = parseDateKey(dateKey)
        val month = monthNamesShort[cal.get(Calendar.MONTH).coerceIn(0, 11)]
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%s %02d", month, day)
    }

    fun formatFullDateHeader(dateKey: String): String {
        val cal = parseDateKey(dateKey)
        val dow = dayOfWeekFull[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        val month = monthNamesFull[cal.get(Calendar.MONTH).coerceIn(0, 11)]
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val year = cal.get(Calendar.YEAR)
        return "$dow, $month $day, $year"
    }

    fun formatRelativeLabel(dateKey: String): String {
        val today = todayDateKey()
        if (dateKey == today) return "TODAY"
        val yesterday = addDays(today, -1)
        if (dateKey == yesterday) return "YESTERDAY"
        val tomorrow = addDays(today, 1)
        if (dateKey == tomorrow) return "TOMORROW"

        val todayCal = parseDateKey(today)
        val targetCal = parseDateKey(dateKey)
        val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
        val diffDays = (diffMillis / (1000L * 60 * 60 * 24)).toInt()
        return when {
            diffDays < 0 -> "${-diffDays}d ago"
            else -> "In ${diffDays}d"
        }
    }

    fun formatMinutesToTime(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 1440)
        val h24 = (clamped / 60) % 24
        val m = clamped % 60
        return String.format(Locale.US, "%02d:%02d", h24, m)
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun monthName(monthZeroIndexed: Int): String =
        monthNamesFull[monthZeroIndexed.coerceIn(0, 11)]

    fun monthNameShort(monthZeroIndexed: Int): String =
        monthNamesShort[monthZeroIndexed.coerceIn(0, 11)]

    fun getMonthGridDays(year: Int, monthZeroIndexed: Int): List<String?> {
        val cal = Calendar.getInstance()
        cal.set(year, monthZeroIndexed, 1, 12, 0, 0)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = ArrayList<String?>(firstDayOfWeek + daysInMonth)
        repeat(firstDayOfWeek) { cells.add(null) }
        for (day in 1..daysInMonth) {
            cells.add(String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroIndexed + 1, day))
        }
        return cells
    }

    fun getSupportedYearRange(): IntRange {
        val center = currentYear()
        return (center - 10)..(center + 10)
    }

    fun getTwentyYearHorizon(): List<Int> {
        return getSupportedYearRange().toList()
    }
}

data class DayChipInfo(
    val dateKey: String,
    val dayOfMonth: Int,
    val dayOfWeekShort: String,
    val monthShort: String,
    val year: Int,
    val isToday: Boolean
)
