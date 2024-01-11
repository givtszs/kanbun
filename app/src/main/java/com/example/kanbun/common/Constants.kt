package com.example.kanbun.common

enum class FirestoreCollection(val collectionName: String) {
    USERS("users"),
    WORKSPACES("workspaces")
}

enum class AuthProvider(val providerId: String) {
    EMAIL("password"),
    GOOGLE("google.com"),
    GITHUB("github.com")
}

const val EMAIL_RESEND_TIME_LIMIT = 60