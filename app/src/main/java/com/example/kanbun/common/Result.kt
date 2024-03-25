package com.example.kanbun.common

sealed class Result<out T : Any> {
    class Success<out T : Any>(val data: T) : Result<T>()
    class Error(val message: String?, val e: Throwable? = null) : Result<Nothing>()
}

inline fun <T, R : Any> T.runCatching(block: T.() -> R): Result<R> {
    return try {
        Result.Success(block())
    } catch (e: Throwable) {
       Result.Error(e.message, e)
    }
}