package com.example.kanbun.ui.custom_views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.common.tagColors
import com.google.android.material.card.MaterialCardView

class TagColorView(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var colorCard: MaterialCardView
        private set

    init {
        inflate(context, R.layout.item_color_preview, this)
        colorCard = findViewById(R.id.cardColor)
    }

    fun bind(@ColorRes color: Int) {
        colorCard.setBackgroundColor(getColor(context, color))
    }
}