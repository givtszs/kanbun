package com.example.kanbun.domain.utils

interface ConnectivityChecker {
    fun hasInternetConnection(): Boolean
}