package com.example.kanbun.domain.utils

interface EmailPatternValidator {
    fun isEmailPatternValid(email: String): Boolean
}