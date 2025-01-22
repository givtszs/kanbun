package com.example.kanbun.data

import android.util.Patterns
import com.example.kanbun.domain.utils.EmailPatternValidator

class AndroidEmailPatternValidator : EmailPatternValidator {
    override fun isEmailPatternValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}