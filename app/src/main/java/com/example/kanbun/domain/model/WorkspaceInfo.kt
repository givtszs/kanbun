package com.example.kanbun.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WorkspaceInfo(
    val id: String = "",
    val name: String = ""
) : Parcelable