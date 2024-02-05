package com.example.kanbun.domain.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@JsonClass(generateAdapter = true)
data class Task(
    val id: String = "",
    val position: Long = 0,
    val name: String = "",
    val description: String = "",
) : Parcelable
