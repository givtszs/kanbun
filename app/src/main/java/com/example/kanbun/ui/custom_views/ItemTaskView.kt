package com.example.kanbun.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.kanbun.R
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.card.MaterialCardView

class ItemTaskView(
    context: Context, attrSet: AttributeSet
) : MaterialCardView(context, attrSet) {

    var task: ConstraintLayout
        private set
    var dropArea: LinearLayout
        private set

    var taskName: TextView
        private set

    var taskDescription: TextView
        private set

    var taskTags: FlexboxLayout
        private set

    var taskDate: TextView
        private set

    var taskHorSeparator: View
     private set

    init {
        inflate(context, R.layout.item_task_view, this)
        task = findViewById(R.id.task)
        dropArea = findViewById(R.id.dropArea)
        taskName = findViewById(R.id.taskName)
        taskDescription = findViewById(R.id.taskDescription)
        taskTags = findViewById(R.id.taskTags)
        taskDate = findViewById(R.id.taskDate)
        taskHorSeparator = findViewById(R.id.taskHorSeparator)
    }
}