package com.example.kanbun.presentation


interface StateHandler {
    fun collectState()
    fun processState(state: ViewState)
}