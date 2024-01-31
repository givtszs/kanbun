package com.example.kanbun.data.model

import androidx.resourceinspection.annotation.Attribute.IntMap

data class FirestoreTask(
    val position: Long = 0,
    val name: String = "",
    val description: String = ""
)