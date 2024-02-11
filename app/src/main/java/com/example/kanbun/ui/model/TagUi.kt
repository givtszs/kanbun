package com.example.kanbun.ui.model

import com.example.kanbun.domain.model.Tag

data class TagUi(
    val tag: Tag,
    var isSelected: Boolean = false
    )