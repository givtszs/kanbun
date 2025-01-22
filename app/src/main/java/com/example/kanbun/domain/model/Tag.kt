package com.example.kanbun.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: String = "",
    val name: String = "",
    val color: String = ""
) : Parcelable {

    fun getBackgroundColor(): String {
        return "#33${color.substringAfter("#")}" // 33 - 20% alpha value
    }
}