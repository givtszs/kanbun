package com.example.kanbun.common

sealed class Result<out T : Any> {
    class Success<out T : Any>(val data: T) : Result<T>()
    class Error(val message: String) : Result<Nothing>()
    class Exception(val message: String?, val e: Throwable) : Result<Nothing>()
}
