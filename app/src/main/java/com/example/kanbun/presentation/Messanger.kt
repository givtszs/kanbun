package com.example.kanbun.presentation

data class Messanger(
    var message: String? = null
) {
    fun messageShown() {
        message = null
    }
}