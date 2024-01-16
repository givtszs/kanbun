package com.example.kanbun.presentation

data class Messanger(
    var message: String? = null
) {
    fun showMessage(message: String?) = copy(message = message)

    fun messageShown() {
        message = null
    }
}