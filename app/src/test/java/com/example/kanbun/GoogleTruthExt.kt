package com.example.kanbun

import com.example.kanbun.common.Result
import com.google.common.truth.Subject

fun Subject.isResultSuccess() = isInstanceOf(Result.Success::class.java)
fun Subject.isResultError() = isInstanceOf(Result.Error::class.java)