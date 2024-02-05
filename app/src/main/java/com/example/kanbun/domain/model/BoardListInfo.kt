package com.example.kanbun.domain.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Stores the [BoardList]'s Firestore reference path and document id
 *
 * @property id the Firestore document id
 * @property path the Firestore reference path
 */
@Parcelize
@JsonClass(generateAdapter = true)
data class BoardListInfo(
    val id: String = "",
    val path: String = ""
) : Parcelable