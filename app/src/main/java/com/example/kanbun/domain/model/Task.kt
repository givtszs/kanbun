package com.example.kanbun.domain.model

import android.content.Context
import android.os.Parcelable
import com.example.kanbun.R
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.convertTimestampToDateString
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Task(
    val id: String = "",
    val position: Long = 0,
    val name: String = "",
    val description: String = "",
    val author: String = "", // the creator's name
    val tags: List<String> = emptyList(), // list of tag ids
    val members: List<String> = emptyList(), // list of user ids
    val dateStarts: Long? = null, // probably will be stored as the timestamp
    val dateEnds: Long? = null
) : Parcelable{
    fun getDisplayDate(context: Context): String? {
        return when {
            dateStarts != null && dateEnds != null ->
                context.resources.getString(
                    R.string.task_date,
                    convertTimestampToDateString(DATE_TIME_FORMAT, dateStarts),
                    convertTimestampToDateString(DATE_TIME_FORMAT, dateEnds)
                )

            dateStarts != null ->
                context.resources.getString(
                    R.string.date_starts,
                    convertTimestampToDateString(
                        DATE_TIME_FORMAT,
                        dateStarts
                    )
                )

            dateEnds != null -> context.resources.getString(
                R.string.date_ends,
                convertTimestampToDateString(
                    DATE_TIME_FORMAT,
                    dateEnds
                )
            )

            else -> null
        }
    }
}
