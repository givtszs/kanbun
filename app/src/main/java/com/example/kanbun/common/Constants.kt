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

object ToastMessages {
    const val NO_NETWORK_CONNECTION = "No Internet connection"
}


const val EMAIL_RESEND_TIME_LIMIT = 60