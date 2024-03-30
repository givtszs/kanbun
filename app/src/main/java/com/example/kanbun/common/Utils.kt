package com.example.kanbun.common

import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.kanbun.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.reflect.typeOf

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

fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(
    combine(flow1, flow2, flow3, ::Triple),
    combine(flow4, flow5, flow6, ::Triple),
) { t1, t2 ->
    transform(
        t1.first,
        t1.second,
        t1.third,
        t2.first,
        t2.second,
        t2.third
    )
}