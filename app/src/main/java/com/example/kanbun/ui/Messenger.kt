package com.example.kanbun.ui

data class Messenger(
    var message: String? = null
) {
    fun showMessage(message: String?) = copy(message = message)

    fun messageShown() {
        message = null
    }
}