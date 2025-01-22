package com.example.kanbun.ui.model
data class ItemRole(
    val name: String,
    val description: String
) {
    override fun toString(): String {
        return name
    }
}