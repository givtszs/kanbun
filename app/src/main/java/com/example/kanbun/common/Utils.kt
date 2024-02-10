package com.example.kanbun.common

import android.content.Context
import android.util.Log
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun getColor(context: Context, @ColorRes color: Int) = ContextCompat.getColor(context, color)

// TODO: Select user's UTC as the base for date/time conversion

fun convertDateStringToTimestamp(format: String, date: String): Long? = try {
    SimpleDateFormat(format, Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(date)
        ?.time
} catch (e: ParseException) {
    Log.e("DateTimeParser", e.message, e)
    null
}

fun convertTimestampToDateString(format: String, timestamp: Long): String = SimpleDateFormat(
    format,
    Locale.getDefault()
).format(timestamp).toString()