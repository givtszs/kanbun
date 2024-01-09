package com.example.kanbun.common

enum class FirestoreCollection(val collectionName: String) {
    USERS("users"),
    WORKSPACES("workspaces")
}

enum class AuthType(val typeName: String) {
    EMAIL("Email"),
    GOOGLE("Google"),
    GITHUB("GitHub")
}

const val EMAIL_RESEND_TIME_LIMIT = 60