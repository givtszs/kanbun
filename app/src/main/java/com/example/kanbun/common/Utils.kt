package com.example.kanbun.common

fun getAuthType(typeName: String): AuthType? {
    return when (typeName) {
        AuthType.GOOGLE.typeName -> AuthType.GOOGLE
        else -> null
    }
}