package com.example.kanbun.domain.model

import android.os.Parcelable
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
) : Parcelable
