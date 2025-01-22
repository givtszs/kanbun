package com.example.kanbun.ui.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.kanbun.R
import com.google.android.material.card.MaterialCardView

class PreferenceView(context: Context, attributeSet: AttributeSet) :
    ConstraintLayout(context, attributeSet) {

    private var card: MaterialCardView

    init {
        inflate(context, R.layout.preference_view, this)

        val attributes = context.theme.obtainStyledAttributes(
            attributeSet, R.styleable.PreferenceView,
            0, 0
        )
        try {
            card = findViewById(R.id.prefCardView)
            findViewById<TextView>(R.id.tvPreferenceName).text =
                attributes.getString(R.styleable.PreferenceView_android_text)

            findViewById<ImageView>(R.id.ivPrefIcon)
                .setImageDrawable(attributes.getDrawable(R.styleable.PreferenceView_android_icon))
        } finally {
            attributes.recycle()
        }

    }

    fun setOnPreferenceClickListener(listener: OnClickListener) {
        card.setOnClickListener(listener)
    }
}