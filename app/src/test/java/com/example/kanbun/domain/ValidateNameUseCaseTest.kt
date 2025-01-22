package com.example.kanbun.domain

import com.example.kanbun.domain.usecase.ValidateNameUseCase
import com.example.kanbun.isResultError
import com.example.kanbun.isResultSuccess
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidateNameUseCaseTest {
    private val validateName = ValidateNameUseCase()

    @Test
    fun validateName_validName_returnsResultSuccess() {
        val name = "Test Name"
        val result = validateName(name)

        assertThat(result).isResultSuccess()
    }

    @Test
    fun validateName_emptyName_returnsResultError() {
        val name = ""
        val result = validateName(name)

        assertThat(result).isResultError()
    }

    @Test
    fun validateName_nameIsTooSmall_returnsResultError() {
        val name = "A"
        val result = validateName(name)

        assertThat(result).isResultError()
    }

    @Test
    fun validateName_nameIsTooBig_returnsResultError() {
        val name =
            "Johnathan Michael Alexander William Benjamin Theodore Christopher David Edward Richard Anthony Joseph Charles Frederick Daniel Matthew Patrick Robert George Henry Samuel Jackson"
        val result = validateName(name)

        assertThat(result).isResultError()
    }

    @Test
    fun validateName_nameContainsNonLetterSymbols_returnsResultError() {
        val name = "Test_1"
        val result = validateName(name)

        assertThat(result).isResultError()
    }

    @Test
    fun validateName_nameDoesNotContainLetters_returnsResultError() {
        val name = "123_"
        val result = validateName(name)

        assertThat(result).isResultError()
    }
}