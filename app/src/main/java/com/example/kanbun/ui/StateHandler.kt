package com.example.kanbun.ui


/**
 * Interface for handling the [ViewState] in a fragment.
 */
interface StateHandler {

    /**
     * Collects the UI state from a state flow
     */
    fun collectState()

    /**
     * Handles and updates the UI based on the received [state]
     */
    fun processState(state: ViewState)
}