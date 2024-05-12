package com.example.kanbun.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.kanbun.R
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.card.MaterialCardView

class TaskView(
    context: Context, attrSet: AttributeSet? = null
) : ConstraintLayout(context, attrSet) {

    var taskCard: MaterialCardView
        private set

    var dropArea: View
        private set

    var taskName: TextView
        private set

    var taskDescription: TextView
        private set

    var taskTags: FlexboxLayout
        private set

    var taskDate: TextView
        private set

    init {
        inflate(context, R.layout.task_view, this)
        taskCard = findViewById(R.id.taskCard)
        dropArea = findViewById(R.id.dropArea)
        taskName = findViewById(R.id.taskName)
        taskDescription = findViewById(R.id.taskDescription)
        taskTags = findViewById(R.id.taskTags)
        taskDate = findViewById(R.id.taskDate)
    }
}