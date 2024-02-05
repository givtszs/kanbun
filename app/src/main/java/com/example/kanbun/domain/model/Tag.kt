package com.example.kanbun.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: String = "",
    val name: String = "",
    val textColor: String = "",
    val backgroundColor: String = ""
) : Parcelable