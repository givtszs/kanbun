package com.example.kanbun.ui.task_tag

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.domain.model.Tag
import com.google.android.material.card.MaterialCardView

class TagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    isBig: Boolean = true
) : ConstraintLayout(context, attrs) {
    private var cardTag: MaterialCardView
    private var tvTag: TextView

    init {
        if (isBig) {
            LayoutInflater.from(context).inflate(R.layout.item_task_tag_big, this, true)
            tvTag = findViewById(R.id.tvTagBig)
            cardTag = findViewById(R.id.cardTagBig)
        } else {
            LayoutInflater.from(context).inflate(R.layout.item_task_tag_small, this, true)
            tvTag = findViewById(R.id.tvTagSmall)
            cardTag = findViewById(R.id.cardTagSmall)
        }
    }

    fun setOnCardClickListener(listener: OnClickListener) {
        cardTag.setOnClickListener(listener)
    }

    fun bind(tag: Tag, isClickable: Boolean, isSelected: Boolean) {
        tvTag.apply {
            text = tag.name
            setTextColor(Color.parseColor(tag.textColor))
        }

        cardTag.apply {
            this.isClickable = isClickable
            setCardBackgroundColor(Color.parseColor(tag.backgroundColor))
            strokeColor = if (isSelected) {
                getColor(context, R.color.md_theme_light_primary)
            } else {
                getColor(context, R.color.white)
            }
        }
    }
}