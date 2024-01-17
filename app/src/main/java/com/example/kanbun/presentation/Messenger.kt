package com.example.kanbun.presentation

data class Messenger(
    var message: String? = null
) {
    fun showMessage(message: String?) = copy(message = message)

    fun messageShown() {
        message = null
    }
}