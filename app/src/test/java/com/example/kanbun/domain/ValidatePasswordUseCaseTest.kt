package com.example.kanbun.domain

import com.example.kanbun.domain.usecase.ValidatePasswordUseCase
import com.example.kanbun.isResultError
import com.example.kanbun.isResultSuccess
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidatePasswordUseCaseTest {
    private val validatePassword = ValidatePasswordUseCase()

    @Test
    fun validatePassword_validPassword_returnsResultSuccess() {
        val password = "Qwer1."
        val result = validatePassword(password)

        assertThat(result).isResultSuccess()
    }

    @Test
    fun validatePassword_tooShortPassword_returnsResultError() {
        val password = "Qwer1"
        val result = validatePassword(password)

        assertThat(result).isResultError()
    }

    @Test
    fun validatePassword_containsWhitespace_returnsResultError() {
        val password = "Qwer 1"
        val result = validatePassword(password)

        assertThat(result).isResultError()
    }

    @Test
    fun validatePassword_noNumbers_returnsResultError() {
        val password = "Qwerty"
        val result = validatePassword(password)

        assertThat(result).isResultError()
    }

    @Test
    fun validatePassword_noLetters_returnsResultError() {
        val password = "1853_4"
        val result = validatePassword(password)

        assertThat(result).isResultError()
    }

    @Test
    fun validatePassword_noUppercaseLetters_returnsResultError() {
        val password = "qwerty1."
        val result = validatePassword(password)

        assertThat(result).isResultError()
    }

    @Test
    fun validatePassword_noLowercaseLetters_returnsResultError() {
        val password = "QWER1.TY"
        val result = validatePassword(password)

        assertThat(result).isResultError()
    }

    @Test
    fun validatePassword_noSpecialCharacters_returnsResultError() {
        val password = "Qwerty123"
        val result = validatePassword(password)

        assertThat(result).isResultError()
    }
}