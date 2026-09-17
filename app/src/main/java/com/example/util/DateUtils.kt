package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }

    fun formatIso(timestamp: Long): String {
        return isoFormat.format(Date(timestamp))
    }

    fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun endOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun todayRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return Pair(startOfDay(now), endOfDay(now))
    }

    fun yesterdayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return Pair(startOfDay(cal.timeInMillis), endOfDay(cal.timeInMillis))
    }

    fun thisWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = endOfDay(cal.timeInMillis)
        return Pair(startOfWeek, endOfWeek)
    }

    fun thisMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        val endOfMonth = endOfDay(cal.timeInMillis)
        return Pair(startOfMonth, endOfMonth)
    }

    fun monthRangeFor(monthOffset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        val endOfMonth = endOfDay(cal.timeInMillis)
        return Pair(startOfMonth, endOfMonth)
    }

    fun customRange(fromMillis: Long, toMillis: Long): Pair<Long, Long> {
        val start = startOfDay(kotlin.math.min(fromMillis, toMillis))
        val end = endOfDay(kotlin.math.max(fromMillis, toMillis))
        return Pair(start, end)
    }
}
