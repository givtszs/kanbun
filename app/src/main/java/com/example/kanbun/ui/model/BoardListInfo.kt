package com.example.kanbun.ui.model

import com.example.kanbun.domain.model.BoardList
import com.squareup.moshi.JsonClass

/**
 * Stores the [BoardList]'s Firestore reference path and document id
 *
 * @property id the Firestore document id
 * @property path the Firestore reference path
 */
@JsonClass(generateAdapter = true)
data class BoardListInfo(
    val id: String,
    val path: String
)