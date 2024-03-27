package com.example.kanbun.common

import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.kanbun.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun getColor(context: Context, @ColorRes color: Int) = ContextCompat.getColor(context, color)

// TODO: Select user's UTC as the base for date/time conversion

fun convertDateStringToTimestamp(format: String, date: String): Long? = try {
    SimpleDateFormat(format, Locale.getDefault())
        .apply { timeZone = TimeZone.getDefault() }
        .parse(date)
        ?.time
} catch (e: ParseException) {
    Log.e("DateTimeParser", e.message, e)
    null
}

fun convertTimestampToDateString(format: String, timestamp: Long?): String = try {
    SimpleDateFormat(format, Locale.getDefault())
        .apply { timeZone = TimeZone.getDefault() }
        .format(timestamp).toString()
} catch (e: IllegalArgumentException) {
    "dd/mm/yyyy, hh:mm"
}

fun loadUserProfilePicture(context: Context, pictureUrl: String?, view: ImageView) {
    Glide.with(context)
        .load(pictureUrl)
        .placeholder(R.drawable.ic_launcher_background)
        .into(view)
}