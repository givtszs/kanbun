package com.example.kanbun.presentation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

open class BaseFragment : Fragment() {
    protected fun navigate(destinationId: Int) {
        findNavController().navigate(destinationId)
    }
}