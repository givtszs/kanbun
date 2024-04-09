package com.example.kanbun.common

sealed class Result<T> {
    class Success<T>(val data: T) : Result<T>()
    class Error<T>(val message: String?, val e: Throwable? = null) : Result<T>()

    fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) {
            block(data)
        }
        return this
    }

    fun onError(block: (String?, Throwable?) -> Unit): Result<T> {
        if (this is Result.Error) {
            block(message, e)
        }
        return this
    }
}



inline fun <T, R> T.runCatching(block: T.() -> R): Result<R> {
    return try {
        Result.Success(block())
    } catch (e: Throwable) {
       Result.Error(e.message, e)
    }
}