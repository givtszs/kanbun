package com.example.kanbun.common

enum class FirestoreCollection(val collectionName: String) {
    USERS("users"),
    WORKSPACES("workspaces")
}

enum class AuthType(val typeName: String) {
    GOOGLE("Google")
}