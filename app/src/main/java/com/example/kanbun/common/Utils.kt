package com.example.kanbun.common

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

fun getColor(context: Context, @ColorRes color: Int) = ContextCompat.getColor(context, color)