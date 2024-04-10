package com.example.kanbun.domain

import com.example.kanbun.domain.usecase.ValidateEmailUseCase
import com.example.kanbun.domain.utils.EmailPatternValidator
import com.example.kanbun.isResultError
import com.example.kanbun.isResultSuccess
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ValidateEmailUseCaseTest {
    private lateinit var emailPatternValidator: EmailPatternValidator
    private lateinit var validateEmail: ValidateEmailUseCase

    @Before
    fun setUp() {
        emailPatternValidator = Mockito.mock(EmailPatternValidator::class.java)
        validateEmail = ValidateEmailUseCase(emailPatternValidator)
    }

    @Test
    fun validateEmail_validEmail_returnsResultSuccess() {
        val email = "i_am.valid@mail.co"
        `when`(emailPatternValidator.isEmailPatternValid(email)).thenReturn(true)
        val result = validateEmail(email)

        assertThat(result).isResultSuccess()
    }

    @Test
    fun validateEmail_emptyEmail_returnsResultError() {
        val email = ""
        val result = validateEmail(email)

        assertThat(result).isResultError()
    }

    @Test
    fun validateEmail_tooShortEmail_returnsResultError() {
        val email = "q@mail.com"
        `when`(emailPatternValidator.isEmailPatternValid(email)).thenReturn(false)
        val result = validateEmail(email)

        assertThat(result).isResultError()
    }

    @Test
    fun validateEmail_noDomain_returnsResultError() {
        val email = "test@"
        `when`(emailPatternValidator.isEmailPatternValid(email)).thenReturn(false)
        val result = validateEmail(email)

        assertThat(result).isResultError()
    }

    @Test
    fun validateEmail_noAtSymbol_returnsResultError() {
        val email = "testmail.co"
        `when`(emailPatternValidator.isEmailPatternValid(email)).thenReturn(false)
        val result = validateEmail(email)

        assertThat(result).isResultError()
    }

    @Test
    fun validateEmail_emailStartsWithDot_returnsResultError() {
        val email = ".test@mail.co"
        `when`(emailPatternValidator.isEmailPatternValid(email)).thenReturn(false)
        val result = validateEmail(email)

        assertThat(result).isResultError()
    }

    @Test
    fun validateEmail_emailHasDoubleAtSymbol_returnsResultError() {
        val email = "qatesteverything@@gmail.com"
        `when`(emailPatternValidator.isEmailPatternValid(email)).thenReturn(false)
        val result = validateEmail(email)

        assertThat(result).isResultError()
    }
}