package com.example.kanbun.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: String = "",
    val name: String = "",
    val colorId: Int = -1
) : Parcelable