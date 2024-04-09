package com.example.kanbun.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.kanbun.R
import com.google.android.material.card.MaterialCardView

class PreferenceView(context: Context, attributeSet: AttributeSet) :
    ConstraintLayout(context, attributeSet) {

    private lateinit var card: MaterialCardView

    init {
        inflate(context, R.layout.preference_view, this)

        val attributes = context.theme.obtainStyledAttributes(
            attributeSet, R.styleable.PreferenceView,
            0, 0
        )
        try {
            findViewById<TextView>(R.id.tvPreferenceName).apply {
                text = attributes.getString(R.styleable.PreferenceView_android_text)
            }
            card = findViewById(R.id.prefCardView)
        } finally {
            attributes.recycle()
        }

    }

    fun setOnPreferenceClickListener(listener: OnClickListener) {
        card.setOnClickListener(listener)
    }
}